'use strict';

/**
 * @ngdoc function
 * @name oncokbApp.controller:NavCtrl
 * @description
 * # NavCtrl
 * Controller of the oncokbApp
 */
angular.module('oncokb')
.controller('NavCtrl', function ($scope, $location, config, gapi) {
    $scope.signinCallback = function(result) {
        console.log('auto signedIn result',result);
    };

    $scope.authorize = function(){
        storage.requireAuth(false).then(function (result) {
            if(result.status && result.status.signed_in) {
                $scope.signedIn = true;
            }else{
                $scope.signedIn = false;
            }
        });
    };
// This flag we use to show or hide the button in our HTML.
    $scope.signedIn = false;
    $scope.user = {};
    $scope.user.email = '';
    $scope.user.avatar = '';
    // Here we do the authentication processing and error handling.
    // Note that authResult is a JSON object.
    $scope.processAuth = function(authResult) {
        // Do a check if authentication has been successful.
        console.log(gapi.auth.getToken());
        if(authResult['access_token']) {
            // Successful sign in.
            $scope.signedIn = true;

            $scope.getUserInfo();
        } else if(authResult['error']) {
            // Error while signing in.
            $scope.signedIn = false;

            // Report error.
        }
    };

    // When callback is received, we need to process authentication.
    $scope.signInCallback = function(authResult) {
        $scope.processAuth(authResult);
    };

    // Render the sign in button.
    $scope.renderSignInButton = function() {
        gapi.signin.render('signInButton',
            {
                'callback': $scope.signInCallback, // Function handling the callback.
                'clientid': config.clientId, // CLIENT_ID from developer console which has been explained earlier.
                // 'requestvisibleactions': 'http://schemas.google.com/AddActivity', // Visible actions, scope and cookie policy wont be described now,
                                                                                  // as their explanation is available in Google+ API Documentation.
                'scope': config.scopes.join(' '),
                'cookiepolicy': 'single_host_origin'
            }
        );
    };

    // Process user info.
    // userInfo is a JSON object.
    $scope.processUserInfo = function(userInfo) {
        // You can check user info for domain.
        if(userInfo['domain'] == 'mycompanydomain.com') {
            // Hello colleague!
        }

        if(userInfo.emails) {
            for (var i = 0; i < userInfo.emails.length; i++) {
                if(userInfo.emails[i].type === 'account'){
                    $scope.user.email = userInfo.emails[i].value;
                    break;
                }
            }
        }
        if(userInfo.image && userInfo.image.url) {
            $scope.user.avatar = userInfo.image.url;
        }
        $scope.$apply();
    }
     
    $scope.signOut = function() {
        console.log('clicked');
        $scope.$eval(gapi.auth.signOut());
    };
    // When callback is received, process user info.
    $scope.userInfoCallback = function(userInfo) {
        $scope.processUserInfo(userInfo);
    };

    // Request user info.
    $scope.getUserInfo = function() {
        // gapi.client.request(
        //     {
        //         'path':'/plus/v1/people/me',
        //         'method':'GET',
        //         'callback': $scope.userInfoCallback
        //     }
        // );
        gapi.client.load('plus','v1', function(){
            gapi.client.plus.people.get({
                'userId' : 'me'
            }).execute($scope.userInfoCallback);
        });
    };

    // Start function in this example only renders the sign in button.
    $scope.start = function() {
        $scope.renderSignInButton();
    };

    $scope.tabIsActive = function(route) {
        if( route instanceof Array) {
            for (var i = route.length - 1; i >= 0; i--) {
                if(route[i] === $location.path()) {
                    return true;
                }
            }
            return false;
        }else {
            return route === $location.path();
        }
    };

    // Call start function on load.
    $scope.init = $scope.renderSignInButton();

});
