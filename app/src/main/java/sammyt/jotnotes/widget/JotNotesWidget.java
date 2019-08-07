package sammyt.jotnotes.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import sammyt.jotnotes.EditActivity;
import sammyt.jotnotes.R;

/**
 * Implementation of App Widget functionality.
 */
public class JotNotesWidget extends AppWidgetProvider {

    private static final int REQ_EDIT_ACTIVITY = 80619;
    private static final int REQ_REFRESH_WIDGETS = 80719;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, int[] allWidgetIds) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.jot_notes_widget);

        // Set up the intent that will provide the views for this collection
        Intent intent = new Intent(context, JotNotesWidgetService.class);

        // Add the app widget ID to the extras
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects
        // to a RemoteViewsService  through the specified intent.
        // This is how you populate the data.
        views.setRemoteAdapter(R.id.widget_list, intent);

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        views.setEmptyView(R.id.widget_list, R.id.empty_view);

        // Set up the Pending Intent Template to use with our Widget Service's Fill-in Intent
        Intent editIntent = new Intent(context, EditActivity.class);
        PendingIntent editPendingIntent = PendingIntent.getActivity(context, REQ_EDIT_ACTIVITY,
                editIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setPendingIntentTemplate(R.id.widget_list, editPendingIntent);

        // Use the same Edit Activity pending intent to open up a blank activity
        // when the button is clicked
        views.setOnClickPendingIntent(R.id.widget_add_note, editPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, appWidgetIds);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        if(AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())){
            // Request an update to the App Widget Collection
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, JotNotesWidget.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
        }
    }
}

