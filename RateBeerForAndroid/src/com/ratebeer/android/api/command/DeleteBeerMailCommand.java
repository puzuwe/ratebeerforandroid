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

import com.ratebeer.android.api.ApiMethod;
import com.ratebeer.android.api.Command;
import com.ratebeer.android.api.RateBeerApi;
import com.ratebeer.android.app.persistance.BeerMail;

public class DeleteBeerMailCommand extends Command {
	
	private final BeerMail mail;

	public DeleteBeerMailCommand(RateBeerApi api, BeerMail mail) {
		super(api, ApiMethod.DeleteBeerMail);
		this.mail = mail;
	}

	public BeerMail getMail() {
		return mail;
	}

}
