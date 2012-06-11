/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.widget;

import com.nononsenseapps.notepad.MainActivity;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.ui.DateView;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.audiofx.BassBoost.Settings;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

/**
 * This is the service that provides the factory to be bound to the collection
 * service.
 */
public class ListWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
	}
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private Context mContext;
	private Cursor mCursor;
	private int mAppWidgetId;

	private static final String indent = "    ";

	private long listId = -1;

	private static final String[] PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_LIST,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_GTASKS_STATUS };

	public ListRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	public void onCreate() {
	}

	public void onDestroy() {
		if (mCursor != null) {
			mCursor.close();
		}
	}

	public int getCount() {
		if (mCursor != null)
			return mCursor.getCount();
		else
			return 0;
	}

	public RemoteViews getViewAt(int position) {
		// Get widget settings
					SharedPreferences settings = mContext.getSharedPreferences(
							ListWidgetConfigure.getSharedPrefsFile(mAppWidgetId),
							Context.MODE_PRIVATE);
					
		// Get the data for this position from the content provider
		String title = "";
//		String note = "";
//		String space = "";
		CharSequence dueDate = "";
		long noteId = -1;
		//long localListId = -1;
		if (mCursor.moveToPosition(position)) {
			final int titleIndex = mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
			final int dateIndex = mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
//			final int noteIndex = mCursor
//					.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
//			final int listIndex = mCursor
//					.getColumnIndex(NotePad.Notes.COLUMN_NAME_LIST);
//			final int indentIndex = mCursor
//					.getColumnIndex(NotePad.Notes.COLUMN_NAME_INDENTLEVEL);
			final int idIndex = mCursor.getColumnIndex(NotePad.Notes._ID);
			title = mCursor.getString(titleIndex);
			//note = mCursor.getString(noteIndex);
			noteId = mCursor.getLong(idIndex);
			//localListId = mCursor.getLong(listIndex);
			String date = mCursor.getString(dateIndex);

//			if (settings != null) {
//				String sortChoice = settings.getString(
//						ListWidgetConfigure.KEY_SORT_TYPE,
//						MainPrefs.DUEDATESORT);
//				if (sortChoice.equals(MainPrefs.POSSUBSORT)) {
//					int indentLevel = mCursor.getInt(indentIndex);
//					int l;
//					for (l = 0; l < indentLevel; l++) {
//						space += indent;
//					}
//				}
//			}

			if (date == null || date.length() == 0)
				dueDate = "";
			else {
				dueDate = DateView.toDate(date);
			}
		}

		final int itemId;
		if (settings!= null && ListWidgetConfigure.THEME_DARK.equals(settings.getString(ListWidgetConfigure.KEY_THEME, ListWidgetConfigure.THEME_LIGHT))) {
			itemId = R.layout.widgetlist_item_dark;
		} else {
			itemId = R.layout.widgetlist_item;
		}
		RemoteViews rv = new RemoteViews(mContext.getPackageName(), itemId);
		rv.setTextViewText(R.id.widget_itemTitle, title);
		//rv.setTextViewText(R.id.widget_itemNote, note);
		rv.setTextViewText(R.id.widget_itemDate, dueDate);
		//rv.setTextViewText(R.id.widget_itemIndent, space);

		// Set the click intent so that we can handle it and show a toast
		// message
		
		long listId = Long.parseLong(settings.getString(
				ListWidgetConfigure.KEY_LIST,
				Integer.toString(MainActivity.ALL_NOTES_ID)));
		
		final Intent fillInIntent = new Intent();
		final Bundle extras = new Bundle();
		extras.putLong(ListWidgetProvider.EXTRA_NOTE_ID, noteId);
		extras.putLong(ListWidgetProvider.EXTRA_LIST_ID, listId);
		fillInIntent.putExtras(extras);
		rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

		return rv;
	}

	public RemoteViews getLoadingView() {
		// We aren't going to return a default loading view in this sample
		return null;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public long getItemId(int position) {
		return position;
	}

	public boolean hasStableIds() {
		return true;
	}

	public void onDataSetChanged() {
		// Refresh the cursor
		if (mCursor != null) {
			mCursor.close();
		}

		// Get widget settings
		SharedPreferences settings = mContext.getSharedPreferences(
				ListWidgetConfigure.getSharedPrefsFile(mAppWidgetId),
				Context.MODE_PRIVATE);
		if (settings != null) {
			listId = Long.parseLong(settings.getString(
					ListWidgetConfigure.KEY_LIST,
					Integer.toString(MainActivity.ALL_NOTES_ID)));

			// getListTitle(settings, listId);

			String sortChoice = settings.getString(
					ListWidgetConfigure.KEY_SORT_TYPE, MainPrefs.DUEDATESORT);
			String sortOrder = NotePad.Notes.ALPHABETIC_SORT_TYPE;

			if (MainPrefs.DUEDATESORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.DUEDATE_SORT_TYPE;
			} else if (MainPrefs.TITLESORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.ALPHABETIC_SORT_TYPE;
			} else if (MainPrefs.MODIFIEDSORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.MODIFICATION_SORT_TYPE;
			} else if (MainPrefs.POSSUBSORT.equals(sortChoice)) {
				sortOrder = NotePad.Notes.POSSUBSORT_SORT_TYPE;
			}

			sortOrder += " "
					+ settings.getString(ListWidgetConfigure.KEY_SORT_ORDER,
							NotePad.Notes.DEFAULT_SORT_ORDERING);

			String listWhere = null;
			String[] listArg = null;
			if (listId > -1) {
				listWhere = NotePad.Notes.COLUMN_NAME_LIST + " IS ? AND "
						+ NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " IS ?";
				listArg = new String[] {
						Long.toString(listId),
						mContext.getText(R.string.gtask_status_uncompleted)
								.toString() };
			} else {
				listWhere = NotePad.Notes.COLUMN_NAME_GTASKS_STATUS + " IS ?";
				listArg = new String[] { mContext.getText(
						R.string.gtask_status_uncompleted).toString() };
			}

			mCursor = mContext.getContentResolver().query(
					NotePad.Notes.CONTENT_VISIBLE_URI, PROJECTION, listWhere,
					listArg, sortOrder);
		}
	}
}