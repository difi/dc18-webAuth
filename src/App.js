import React, {Component} from 'react';
import _ from 'lodash';

import {getDecodedObject, getEncodedObject, getCreateCredentialsOptions, getFetchOptions, getRegisterResponseObject} from "./webauthn/utils";
import webAuthnConfig from './webauthn/config';
import {ContentTypes, ServerSchemes} from "./webauthn/enums";

import logo from './logo.svg';
import './App.css';




class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      username: Math.random(),
    };

    this.sendOptionsRequest = this.sendOptionsRequest.bind(this);
    this.generateCreateCredentials = this.generateCreateCredentials.bind(this);
    this.sendCredentialsToServer = this.sendCredentialsToServer.bind(this);
  }

  sendOptionsRequest() {
    const data = {
      username: this.state.username,
      displayName: this.state.username,
    };
    fetch(webAuthnConfig.registerChallengeEndpoint, getFetchOptions(data, ContentTypes.URLENCODED)).then((response) => {
      if (response.ok) return response.json();
      else throw new Error(response.statusText);
    }).then((data) => {
      this.setState({registerResponse: data.request.publicKeyCredentialCreationOptions});
      this.setState({requestId: data.request.requestId});
    }).catch(console.error);
  }

  generateCreateCredentials() {
    console.log('this.state.registerResponse');
    console.log(this.state.registerResponse);
    console.log('this.state.registerResponse decoded');
    console.log(getDecodedObject(this.state.registerResponse, ['challenge', 'user.id']));
    navigator.credentials.create(getCreateCredentialsOptions(getDecodedObject(this.state.registerResponse, ['challenge', 'user.id']))).then((credentials) => {
      console.log('Credentials from browser:');
      console.log(credentials);
      console.log('Credentials from browser _.toPlainObject(credentials):');
      console.log(_.toPlainObject(credentials));
      this.setState({publicKeyCredential: credentials});
      this.setState({publicKeyCredentialEncoded: getEncodedObject(credentials, ['response.attestationObject', 'response.clientDataJSON', 'rawId'])});
    }).catch(console.error);
  }

  sendCredentialsToServer() {
    console.log('this.state.publicKeyCredentialEncoded');
    console.log(this.state.publicKeyCredentialEncoded);
    let data;
    switch (webAuthnConfig.serverScheme) {
      case ServerSchemes.YUBICO:
        data = {
          credential: this.state.publicKeyCredentialEncoded,
          requestId: this.state.requestId,
        };
        break;
      default:
        data = this.state.publicKeyCredentialEncoded;
    }
    fetch(webAuthnConfig.registerResponseEndpoint, getFetchOptions(data, ContentTypes.JSON)).then((response) => {
      if (response.ok) return response.json();
      else throw new Error(response.statusText);
    }).then((data) => {
      console.log('sendCredentialsToServer response data');
      console.log(data);
      this.setState({registerCredentialResponse: data});
    }).catch(console.error);
  }

  render() {
    return (
      <div className="App">
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo"/>
          <h1 className="App-title">Welcome to React</h1>
        </header>
        <p className="App-intro">
          To get started, edit <code>src/App.js</code> and save to reload.
        </p>
        <button
          onClick={this.sendOptionsRequest}
        >
          Ask server for options
        </button>
        <button
          onClick={this.generateCreateCredentials}
          disabled={!this.state.registerResponse}
        >
          Create credentials with options
        </button>
        <button
          onClick={this.sendCredentialsToServer}
          disabled={!this.state.publicKeyCredentialEncoded}
        >
          Send credentials to server
        </button>

        <div>
          <h4>this.state:</h4>
          <pre
            style={{textAlign: 'left'}}
          >
            {JSON.stringify(this.state, null, 2)}
          </pre>
        </div>
      </div>
    );
  }
}

export default App;
