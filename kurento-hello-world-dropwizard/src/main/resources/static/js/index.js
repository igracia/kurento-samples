/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var videoInput;
var videoOutput;
var webRtcPeer;
var state = null;

function guid() {
	function s4() {
		return Math.floor((1 + Math.random()) * 0x10000)
		.toString(16)
		.substring(1);
	}
	return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
	s4() + '-' + s4() + s4() + s4();
}

var userId = guid(); 

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

window.onload = function() {
	console = new Console();
	console.log('Page loaded ...');
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
	setState(I_CAN_START);
}

window.onbeforeunload = function() {
	stop();
}

function start() {
	console.log('Starting video call ...');

	// Disable start button
	setState(I_AM_STARTING);
	showSpinner(videoInput, videoOutput);

	console.log('Creating WebRtcPeer and generating local sdp offer ...');

	var options = {
			localVideo : videoInput,
			remoteVideo : videoOutput,
			onicecandidate : onIceCandidate
	}
	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
			function(error) {
		if (error)
			return console.error(error);
		webRtcPeer.generateOffer(onOffer);
	});
}

function onOffer(error, sdpOffer) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);

	$.ajax({
		type : "POST",
		data : sdpOffer,
		contentType : "application/json",
		url : location.origin + "/api/hello-world/start/" + userId
	}).done(function(data) {
		if (state === I_AM_STARTING) {
			setState(I_CAN_STOP);
			console.log('SDP answer received from server. Processing ...');

			webRtcPeer.processAnswer(data.sdpAnswer, function(error) {
				if (error)
					return console.error(error);
			});
		}
	}).fail(function(jqXhr, textStatus, errorThrown) {
		stop();
	});

}

function onError(error) {
	console.error(error);
}

function onIceCandidate(candidate) {
	console.log('Local candidate' + JSON.stringify(candidate));

	$.ajax({
		type : "POST",
		data : JSON.stringify(candidate),
		contentType : "application/json",
		url : location.origin + "/api/hello-world/addIceCandidate/" + userId
	}).fail(function(jqXhr, textStatus, errorThrown) {
		stop();
	});

}

function stop() {
	console.log('Stopping video call ...');
	setState(I_CAN_START);
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;

		$.ajax({
			type : "POST",
			url : location.origin + "/api/hello-world/stop/" + userId
		}).then(function() {
			console.log('Session stopped');
		});
	}
	hideSpinner(videoInput, videoOutput);
}

function setState(nextState) {
	switch (nextState) {
	case I_CAN_START:
		$('#start').attr('disabled', false);
		$('#start').attr('onclick', 'start()');
		$('#stop').attr('disabled', true);
		$('#stop').removeAttr('onclick');
		break;

	case I_CAN_STOP:
		$('#start').attr('disabled', true);
		$('#stop').attr('disabled', false);
		$('#stop').attr('onclick', 'stop()');
		break;

	case I_AM_STARTING:
		$('#start').attr('disabled', true);
		$('#start').removeAttr('onclick');
		$('#stop').attr('disabled', true);
		$('#stop').removeAttr('onclick');
		break;

	default:
		onError('Unknown state ' + nextState);
	return;
	}
	state = nextState;
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
