# Code project setup #

The application will be build as a single, open-source Android project. Eclipse is the supported development environment for now and release version are build using Ant. To get the project going:
  * Make sure Eclipse, the Android SDK and the Android Eclipse plugin have been installed, including at least the level 16 (Android 4.1.2) platform. Note: Compatibility from Android 2.1 to Android 4.2+ is supported through the ActionBarSherlock library, which in turn incorporates the Android Support Package.
  * In your development directory run
```
hg clone https://code.google.com/p/ratebeerforandroid
```
  * Open up Eclipse (and a workspace) and use File -> Import -> Existing projects into workspace; then choose Browse and select the `ratebeerforandroid` directory; then tick the check boxes for all five projects (the core, the test project, httpclient, ActionBarSherlock and ViewPagerIndicator).
  * Wait until the workspaces compiles and start/debug!
If it does not compile, check if AndroidAnnotations is enabled and correctly compiles the annotated classes. You might also have to use Project -> Clean on the core project.

# Versioning #

Simply put, only the current release (Google Market) version and the 'next', i.e. the development version matter. The versioning system supports this simple approach:

  * **default** holds the latest stable version
  * **dev** holds the development version in which new/improved feature reside

All normal features are developed directly into **dev**. Locally, the bookmark feature is useful to keep several (potential conflicting) development lines separate. See [this excellent article](http://stevelosh.com/blog/2009/08/a-guide-to-branching-in-mercurial/) on the differences. However, if a major revision (with work between developers) is needed a separate named branch is always possible.

# Screen flow design #

The application's namespace is simply `com.ratebeer.android`. Functionally the app is (using the Launcher or Intents) started into the `Home` activity. A consistent `ActionBar` experience is offered through the ActionBarSherlock project. Slightly different options and layouts are offered to larger devices (read: tablets).

Intents that launch the app (not using LAUNCHER) also go to `Home` in order to start the right activity. Therefore the `Home` activity is responsible to redirect these intents, such as searching or showing a specific beer. For example, [ratebeer.com/b/#/](http://ratebeer.com/b/2/) URL's are picked up by the app and the specific beer with ID # will be shown

Apart from the `SignIn` screen the whole application works form a single `Home` activity, which replaces `Fragment`s instead of starting separate activities. All fragments extend `RateBeerFragment` which provides support for asynchronous retrieving of data, maintaining a status over task progress, managing Google Maps `MapView`s, etc.

Posting (rating, setting drinking status, etc.) information to RateBeer is assumed to be a screen independent operation. Therefore these command are delegated to a background `PosterService` task. This service reports its progress (and failure) to the user using notifications.

# Command execution #

Retrieving of information from and posting to RateBeer.com is done using the `ApiConnection`. This class will synchronously perform GET or POST requests to RateBeer.com based on self-contained `Command`s. These `Command`s are constructed and executed either contained in an asynchronous task (as in `RateBeerActivity`) or directly (as in `PosterService`).

A simple synchronous example:
```
new SetDrinkingStatusCommand(getUser(), "Westvleteren 12").execute();
```
Results, such as a beer's requested details or a list of places, are returned in the original command:
```
CommandResult result = new GetRatingsCommand(getUser(), 4934).execute();
if (result instanceof CommandSuccessResult) {
	for (BeerRating rating : ((GetRatingsCommand)result.getCommand()).getRatings()) {
		System.out.println(rating.userName + " said: " + rating.comments);
	}
} else {
	String error = ((CommandFailureResult)result).getException().toString();
	System.out.println("Problem getting ratings: " + error);
}
```

Fragments that start commands to retrieve information always use the `execute()` method of `RateBeerFragment`, which takes care of performing the task asynchronously and keeping track of progress.

# API or parsing HTML #

Ideally every command executed against RateBeer uses their JSON API. Documentation can be found at http://www.ratebeer.com/json/ratebeer-api.asp and the API key used can be found in the project code.

Unfortunately the API does not offer the full feature set that will be build in RateBeer for Android. Therefore, some command have to be performed directly against the normal web pages by mimicking browser GETs and FORM POSTs and by parsing the response HTML code. In cases where not all desired information is offered by the API, the parsing of HTML is also used instead at the moment. However, since the tiniest change in RateBeer's site can break a full feature of the app (or worse), there should be worked towards no longer relying on this method.

# Code formatting #

In principle the code is formatted according to the Eclipse IDE defaults. There are some small differences:

  * Line wrapping is set to 120 characters
  * Android XML files are formatted according to Eclipse (so check this in Android -> Editors)
  * Comments are wrapped at 120 characters too and - shortly speaking - don't use unnecessary empty lines.

Commenting is highly encouraged but in a meaningful way of course.