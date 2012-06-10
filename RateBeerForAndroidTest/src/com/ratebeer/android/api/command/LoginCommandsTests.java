/*
    This file is part of RateBeer For Android.
    
    RateBeer for Android is free software: you can redistribute it 
    and/or modify it under the terms of the GNU General Public 
    License as published by the Free Software Foundation, either 
    version 3 of the License, or (at your option) any later version.

    RateBeer for Android is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RateBeer for Android.  If not, see 
    <http://www.gnu.org/licenses/>.
 */
package com.ratebeer.android.api.command;

import java.io.IOException;

import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.ratebeer.android.api.ApiException;
import com.ratebeer.android.api.CommandFailureResult;
import com.ratebeer.android.api.CommandResult;
import com.ratebeer.android.api.HttpHelper;
import com.ratebeer.android.api.RateBeerApi;
import com.ratebeer.android.api.UserSettings;
import com.ratebeer.android.api.ApiException.ExceptionType;
import com.ratebeer.android.app.ApplicationSettings;

public class LoginCommandsTests extends AndroidTestCase {

	public void testExecute() {

		// Log in with a valid user
		RateBeerApi api = TestHelper.getApi(getContext(), true);
		try {
			RateBeerApi.ensureLogin(api.getUserSettings());
			// If we are here, everything went fine, since the sign in was successful
			assertTrue(HttpHelper.isSignedIn());
		} catch (IOException e) {
			fail("Sign in failed: " + e.toString());
		} catch (ApiException e) {
			fail("Sign in failed: " + e.getType().toString() + " exception: " + e.toString());
		}
		
		// Sign out now
		SignOutCommand signout = new SignOutCommand(api);
		CommandResult result = signout.execute();
		if (result instanceof CommandFailureResult) {
			fail("Sign out failed: " + ((CommandFailureResult)result).getException().toString());
		}
		// We should be signed out now
		assertTrue(!HttpHelper.isSignedIn());

		// Log in with an invalid user (note: signout needed to be succesful to properly test this!)
		ApplicationSettings settings = new ApplicationSettings(getContext(), PreferenceManager
				.getDefaultSharedPreferences(getContext()));
		settings.saveUserSettings(new UserSettings(156822, "rbandroid", "wrongpassword", null, false));
		RateBeerApi api2 = new RateBeerApi(settings);
		try {
			RateBeerApi.ensureLogin(api2.getUserSettings());
			// We should be signed out still
			assertTrue(!HttpHelper.isSignedIn());
			fail("We should not have been here, since our password was wrong!");
		} catch (IOException e) {
			fail("Login failed: " + e.toString());
		} catch (ApiException e) {
			// Expect an exception here that the authentication failed
			if (e.getType() != ExceptionType.AuthenticationFailed) {
				fail("Login failed: " + e.getType().toString() + " exception: " + e.toString());
			}
		}
		
	}

}