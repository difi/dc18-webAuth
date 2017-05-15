module.exports = function(grunt) {
	grunt.registerTask('dev_sass', [

		'notify:dev_sass'
		, 'clean:dev_sass'
		, 'copy:src_svgcss'
		, 'sass:dev'
		, 'copy:dev_css'
		, 'notify:done'
		]);
};