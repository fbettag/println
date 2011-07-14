/*
 *  Copyright (c) 2011, Franz Bettag <franz@bett.ag>
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * All advertising materials mentioning features or use of this software
 *       must display the following acknowledgement:
 *       This product includes software developed by the Bettag Systems UG
 *       and its contributors.
 *
 *  THIS SOFTWARE IS PROVIDED BY BETTAG SYSTEMS UG ''AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL BETTAG SYSTEMS UG BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

println = {};

/**
 * Upload
 */
println.upload = {};
println.upload.interval = null;
println.upload.started = null;

println.upload.bar = function(form, uuid, cb) {
	if (form === undefined) {
		console.log("Upload-Form not found!");
		return false;
	}

	form = $(form);

	// If we have no UUID, generate one and latch it onto the form action.
	if (uuid == "undefined") {
		uuid = "";
		for (i = 0; i < 32; i++) {
			uuid += Math.floor(Math.random() * 16).toString(16);
		}

		form.attr('action', form.attr('action') + '?X-Progress-ID=' + uuid);

	}

	$('#upload-progress-bar').css('width', 0);
	$('#upload-progress-label').html('uploading');

	println.upload.started = new Date();

	println.upload.progress(uuid, cb);

	return true;
};

println.upload.progress = function(uuid, cb) {
	$.ajax({
		url: '/progress',
		data: 'X-Progress-ID=' + uuid,
		dataType: 'json',
		success: function(upload) {
			var w;

			if (cb && typeof cb == "function") {
				cb(upload);
			}

			if (upload.state === 'done' || upload.state === 'uploading') {
				w = 500 * upload.received / upload.size;
			}

			if (upload.state === 'done') {
				w = 500;
			} else {
				window.setTimeout(function() {
					println.upload.progress(uuid, cb);
				}, 500);
			}

			$('#upload-progress-bar').animate({'width' : w + 'px' }, {queue: false, duration: 1000, complete: function() {
				if (upload.state === 'uploading' && upload.received && upload.size) {
					var uploaded = (100 / upload.size) * upload.received;
					var elapsed = (new Date() - println.upload.started) / 1000;
					var eta = (elapsed * upload.size / upload.received) - elapsed;
					var speed = (upload.received / elapsed);

					if (speed > 1024 * 1024) {
						speed = speed / 1024 / 1024;
						speed = Math.round(speed);
						speed += ' MB/s';
					} else if (speed > 1024) {
						speed = speed / 1024;
						speed = Math.round(speed);
						speed += ' KB/s';
					} else {
						speed = Math.round(speed);
						speed += ' B/s';
					}

					$('#upload-progress-label').html(upload.state + ' <strong>' + Math.round(uploaded) + '%</strong><br /><strong>' + upload.received + '</strong> / <strong>' + upload.size + '</strong> bytes' + '<br /><strong>' + Math.round(eta) + 's</strong> ETA @ ' + speed);
				} else {
					$('#upload-progress-label').html(upload.state);
				}
			}});
		}
	});
};


/**
 * Markup
 */
println.window = {};

println.window.toggle = function(jq) {
	var $d = $(jq);
	$d.dialog($d.dialog("isOpen") ? "close" : "open");
	$('#println_post_name').focus();
};


/**
 * Startup
 */
$(document).ready(function() {
	$("#println-admin").each(function(i, e) {
		$(e).tabs().dialog({
			collapsible: true,
			height: $(window).height() / 2.5,
			width: 700,
			height: 490,
			sticky: true,
			autoOpen: false,
			resizable: false,
			title: "println blogging software"
			// dragStop: wws.cms.resize,
			// open: wws.cms.resize,
			// resize: wws.cms.resize
		});

		// FIXME save window size in cookie
		// FIXME make wmd-textarea auto-height 100% of the window
		// FIXME remove resizable: false from above
		
		// WMD Setup
		$("#println-admin-txtt").wmd({
			"preview": "println_entry_body",
			"saveTo": "println-admin-txtt-cached",
			"helpLink": "http://daringfireball.net/projects/markdown/syntax",
			"helpHoverTitle": "Markdown Help"
		});

		$("#println-admin-txtc").wmd({
			"preview": "println_entry_body",
			"saveTo": "println-admin-txtc-cached",
			"helpLink": "http://daringfireball.net/projects/markdown/syntax",
			"helpHoverTitle": "Markdown Help"
		});
	});


	$(".println_post_tags").each(function(i, e) {
		$(e).tokenInput([
			{id: 7, name: "Ruby"},
			{id: 11, name: "Python"},
			{id: 13, name: "JavaScript"},
			{id: 17, name: "ActionScript"},
			{id: 19, name: "Scheme"},
			{id: 23, name: "Lisp"},
			{id: 29, name: "C#"},
			{id: 31, name: "Fortran"},
			{id: 37, name: "Visual Basic"},
			{id: 41, name: "C"},
			{id: 43, name: "C++"},
			{id: 47, name: "Java"}
		], {
			preventDuplicates: true
		});
	});
	
	
	$("#println-admin form input[type=text], ul.token-input-list").each(function(i, e) {
		// if ($(e).hasClass('println_post_tags')) return;
		$(e).addClass('text ui-widget-content ui-corner-all');
	});

	// Open if requested
	if (/#open/i.test(location.hash)) {
		$("#println-admin").each(function(i, e) {
			println.window.toggle($(e));
		});
	}
});