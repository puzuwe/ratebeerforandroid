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
package com.ratebeer.android.gui.fragments;

import java.util.ArrayList;
import java.util.Date;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.ratebeer.android.R;
import com.ratebeer.android.api.ApiMethod;
import com.ratebeer.android.api.CommandSuccessResult;
import com.ratebeer.android.api.UserSettings;
import com.ratebeer.android.api.command.GetDrinkingStatusCommand;
import com.ratebeer.android.api.command.GetTopBeersCommand.TopListType;
import com.ratebeer.android.api.command.ImageUrls;
import com.ratebeer.android.api.command.Style;
import com.ratebeer.android.gui.SignIn;
import com.ratebeer.android.gui.components.PosterService;
import com.ratebeer.android.gui.components.RateBeerFragment;
import com.ratebeer.android.gui.fragments.SetDrinkingStatusDialogFragment.OnDialogResult;
import com.ratebeer.android.gui.fragments.StylesFragment.StyleAdapter;

public class DashboardFragment extends RateBeerFragment {

	private static final int MENU_SCANBARCODE = 1;
	private static final int MENU_SEARCH = 2;
	private static final int MENU_CALCULATOR = 3;

	private Button drinkingStatus, myProfileButton;
	private ListView stylesView;
	private LayoutInflater inflater;
	private Float density = null;

	public DashboardFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		this.inflater = inflater;
		return inflater.inflate(R.layout.fragment_dashboard, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
				
		// Bind a click listener to the big dashboard buttons
		/*((Button) getView().findViewById(R.id.search)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().onSearchRequested();
			}
		});*/
		myProfileButton = (Button) getView().findViewById(R.id.myprofile);
		myProfileButton.setOnClickListener(onProfileButtonClick());
		((Button) getView().findViewById(R.id.offlineratings)).
			setOnClickListener(onButtonClick(new OfflineRatingsFragment(), false));
		((Button) getView().findViewById(R.id.beerstyles)).
			setOnClickListener(onButtonClick(new StylesFragment(), false));
		((Button) getView().findViewById(R.id.top50)).
			setOnClickListener(onButtonClick(new TopBeersFragment(TopListType.Top50), false));
		((Button) getView().findViewById(R.id.bycountry)).
			setOnClickListener(onButtonClick(new TopBeersFragment(TopListType.TopByCountry), false));
		((Button) getView().findViewById(R.id.places)).
			setOnClickListener(onButtonClick(new PlacesFragment(), false));
		((Button) getView().findViewById(R.id.events)).
			setOnClickListener(onButtonClick(new EventsFragment(), true));
		((Button) getView().findViewById(R.id.beermail)).
			setOnClickListener(onButtonClick(new MailsFragment(), true));
		
		updateProfileImage();
		
		// For tablets, also load the beer styles list
		stylesView = (ListView) getView().findViewById(R.id.styles);
		if (stylesView != null) {
			stylesView.setAdapter(new StylesFragment.StyleAdapter(getActivity(), 
					new ArrayList<Style>(Style.ALL_STYLES.values()), inflater));
			stylesView.setOnItemClickListener(onItemSelected);
		}
		
		// Update drinking status
		drinkingStatus = (Button) getView().findViewById(R.id.drinking_status);
		drinkingStatus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new SetDrinkingStatusDialogFragment(new OnDialogResult() {
					@Override
					public void onSetNewStatus(String newStatus) {
						// Update now drinking status
						Intent i = new Intent(PosterService.ACTION_SETDRINKINGSTATUS);
						i.putExtra(PosterService.EXTRA_NEWSTATUS, newStatus);
						i.putExtra(PosterService.EXTRA_BEERID, PosterService.NO_BEER_EXTRA);
						i.putExtra(PosterService.EXTRA_MESSENGER, new Messenger(new Handler() {
							@Override
							public void handleMessage(Message msg) {
								// Callback from the poster service; just refresh the drinking status
								// if (msg.arg1 == PosterService.RESULT_SUCCESS)
								execute(new GetDrinkingStatusCommand(getRateBeerActivity().getApi()));
							}
						}));
						getActivity().startService(i);
					}
				}).show(getFragmentManager(), "dialog");
			}
		});
		showDrinkingStatus();
		refreshDrinkingStatus();
		
		// Show legal stuff on first app start
		if (getRateBeerApplication().getSettings().isFirstStart()) {
			getRateBeerApplication().getSettings().recordFirstStart();
			getRateBeerActivity().load(new TextInfoFragment(getString(R.string.app_legal_title), getString(R.string.app_legal)));
		}
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (getResources().getConfiguration().screenWidthDp < 800) {
			// For phones, the dashboard & search fragments show a search icon in the action bar
			// Note that tablets always show an search input in the action bar through the HomeTablet activity directly
			MenuItem item = menu.add(Menu.NONE, MENU_SEARCH, Menu.NONE, R.string.home_search);
			item.setIcon(R.drawable.ic_action_search);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
		
		MenuItem item2 = menu.add(Menu.NONE, MENU_SCANBARCODE, Menu.NONE, R.string.search_barcodescanner);
		item2.setIcon(R.drawable.ic_action_barcode);
		item2.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		MenuItem item3 = menu.add(Menu.NONE, MENU_CALCULATOR, Menu.NONE, R.string.home_calculator);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			item3.setIcon(R.drawable.ic_menu_calculator);
		} else {
			item3.setIcon(R.drawable.ic_action_calculator);
		}
		item3.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SEARCH:
			// Open standard search interface
			getActivity().onSearchRequested();
			break;
		case MENU_SCANBARCODE:
	    	// Start the search activity (without specific search string), which offers the actual scanning feature
			getRateBeerActivity().load(new SearchFragment(true));
			break;
		case MENU_CALCULATOR:
			// Start calculator screen
			getRateBeerActivity().load(new CalculatorFragment());
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateProfileImage() {
		if (getRateBeerActivity().getUser() != null) {
			getRateBeerApplication().getImageCache().loadImage(getActivity(),
					ImageUrls.getUserPhotoUrl(getRateBeerActivity().getUser().getUsername()),
					new ImageLoadingListener() {
						@Override
						public void onLoadingStarted() {
						}

						@Override
						public void onLoadingFailed(FailReason arg0) {
						}

						@Override
						public void onLoadingComplete(Bitmap arg0) {
							if (density == null) {
								density = getResources().getDisplayMetrics().density;
							}
							Drawable d = new BitmapDrawable(getResources(), arg0);
							d.setBounds(0, 0, (int) (48 * density), (int) (48 * density));
							myProfileButton.setCompoundDrawables(null, d, null, null);
						}

						@Override
						public void onLoadingCancelled() {
						}
					});
		}
	}

	private OnClickListener onProfileButtonClick() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				UserSettings usr = getRateBeerActivity().getUser();
				if (usr == null) {
					// No user yet, but this is required so start the login screen
					Intent i = new Intent(getActivity(), SignIn.class);
					i.putExtra(SignIn.EXTRA_REDIRECT, true);
					startActivity(i);
				} else {
					getRateBeerActivity().load(new UserViewFragment(usr.getUsername(), usr.getUserID()));
				}
			}
		};
	}

	private OnClickListener onButtonClick(final RateBeerFragment fragment, final boolean requiresUser) {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (requiresUser && getRateBeerActivity().getUser() == null) {
					// No user yet, but this is required so start the login screen
					Intent i = new Intent(getActivity(), SignIn.class);
					i.putExtra(SignIn.EXTRA_REDIRECT, true);
					startActivity(i);
				} else {
					getRateBeerActivity().load(fragment);
				}
			}
		};
	}

	private OnItemClickListener onItemSelected = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Style item = ((StyleAdapter)stylesView.getAdapter()).getItem(position);
			getRateBeerActivity().load(new StyleViewFragment(item));
		}
	};
	
	@Override
	public void onTaskSuccessResult(CommandSuccessResult result) {
		if (getActivity() == null) {
			return;
		}
		if (result.getCommand().getMethod() == ApiMethod.GetDrinkingStatus) {
			GetDrinkingStatusCommand getCommand = (GetDrinkingStatusCommand) result.getCommand();
			// Override the user settings, in which the drinking status is contained
			UserSettings ex = getRateBeerActivity().getSettings().getUserSettings();
			getRateBeerActivity().getSettings().saveUserSettings(new UserSettings(ex.getUserID(), ex.getUsername(), 
					ex.getPassword(), getCommand.getDrinkingStatus(), ex.isPremium(), new Date()));
			showDrinkingStatus();
		}
	}
	
	private void refreshDrinkingStatus() {
		// At max refresh every 5 minutes
		Date d = new Date((new Date()).getTime() - (5 * 60 * 1000)); // = 5 minutes ago
		if (getRateBeerActivity() != null && getRateBeerActivity().getUser() != null && 
				getRateBeerActivity().getUser().getLastDrinkingStatusUpdate().before(d)) {
			execute(new GetDrinkingStatusCommand(getRateBeerActivity().getApi()));
		}	
	}

	public void showDrinkingStatus() {
		if (getRateBeerActivity().getUser() == null || getRateBeerActivity().getUser().getDrinkingStatus() == null || 
				getRateBeerActivity().getUser().getDrinkingStatus().equals("")) {
			drinkingStatus.setVisibility(View.GONE);
		} else {
			drinkingStatus.setVisibility(View.VISIBLE);
			drinkingStatus.setText(getString(R.string.home_nowdrinking, getRateBeerActivity().getUser().getDrinkingStatus()));
		}
	}

}
