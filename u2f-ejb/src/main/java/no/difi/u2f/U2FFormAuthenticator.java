/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.difi.u2f;

import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.SignRequestData;
import com.yubico.u2f.data.messages.SignResponse;
import com.yubico.u2f.exceptions.U2fBadInputException;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class U2FFormAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(U2FCredentialProvider.class);

    private U2F u2f;

    public U2FFormAuthenticator(U2F u2f) {
        this.u2f = u2f;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.debugv("Finish signature, session: {0}", context.getAuthenticationSession().getParentSession().getId());

        try {
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
            String response = formData.getFirst("tokenResponse");

            SignResponse signResponse = SignResponse.fromJson(response);

            SignRequestData signRequestData = SignRequestData.fromJson(context.getAuthenticationSession().getAuthNote("u2f-sign-data"));

            u2f.finishSignature(signRequestData, signResponse, getDeviceRegistrations(context));

            context.success();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.debugv("Sending challenge, session: {0}", context.getAuthenticationSession().getParentSession().getId());

        try {
            String appId = UriUtils.getOrigin(context.getUriInfo().getBaseUri());

            SignRequestData signRequestData = u2f.startSignature(appId, getDeviceRegistrations(context));

            context.getAuthenticationSession().setAuthNote("u2f-sign-data", signRequestData.toJson());

            Response response = context.form()
                    .setAttribute("request", signRequestData)
                    .createForm("fido-u2f-login.ftl");

            context.challenge(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<DeviceRegistration> getDeviceRegistrations(AuthenticationFlowContext context) throws U2fBadInputException {
        List<DeviceRegistration> registrations = new LinkedList<>();
        for (CredentialModel model : context.getSession().userCredentialManager().getStoredCredentialsByType(context.getRealm(), context.getUser(), U2FCredentialProvider.TYPE)) {
            registrations.add(DeviceRegistration.fromJson(model.getValue()));
        }
        return registrations;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, U2FCredentialProvider.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        if (!user.getRequiredActions().contains(U2FRequiredActionProviderFactory.ID)) {
            user.addRequiredAction(U2FRequiredActionProviderFactory.ID);
        }
    }

    @Override
    public void close() {
    }
}
