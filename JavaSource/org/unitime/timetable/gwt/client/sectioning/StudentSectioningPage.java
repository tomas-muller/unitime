/*
 * UniTime 3.2 - 3.5 (University Timetabling Application)
 * Copyright (C) 2010 - 2013, UniTime LLC, and individual contributors
 * as indicated by the @authors tag.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
*/
package org.unitime.timetable.gwt.client.sectioning;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.page.UniTimePageHeader;
import org.unitime.timetable.gwt.client.sectioning.UserAuthentication.UserAuthenticatedEvent;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.UniTimeFrameDialog;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.services.SectioningService;
import org.unitime.timetable.gwt.services.SectioningServiceAsync;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider;
import org.unitime.timetable.gwt.shared.OnlineSectioningInterface.SectioningProperties;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;

/**
 * @author Tomas Muller
 */
public class StudentSectioningPage extends Composite {
	public static final StudentSectioningMessages MESSAGES = GWT.create(StudentSectioningMessages.class);
	public static final StudentSectioningConstants CONSTANTS = GWT.create(StudentSectioningConstants.class);
	
	private final SectioningServiceAsync iSectioningService = GWT.create(SectioningService.class);
	
	public static enum Mode {
		SECTIONING(true),
		REQUESTS(false);
		boolean iSectioning;
		private Mode(boolean isSectioning) { iSectioning = isSectioning; }
		public boolean isSectioning() { return iSectioning; }
	};
	
	public StudentSectioningPage(final Mode mode) {
		final UserAuthentication userAuthentication = new UserAuthentication(UniTimePageHeader.getInstance().getMiddle(), mode.isSectioning() ? !CONSTANTS.isAuthenticationRequired() : false);
		
		if (Window.Location.getParameter("student") == null)
			iSectioningService.whoAmI(new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
					if (!mode.isSectioning() || CONSTANTS.isAuthenticationRequired() || CONSTANTS.tryAuthenticationWhenGuest()) {
						if (CONSTANTS.allowUserLogin())
							userAuthentication.authenticate();
						else if (!mode.isSectioning() || CONSTANTS.isAuthenticationRequired())
							ToolBox.open(GWT.getHostPageBaseURL() + "login.do?target=" + URL.encodeQueryString(Window.Location.getHref()));
						else
							userAuthentication.authenticated(null);
					}
				}
				public void onSuccess(String result) {
					if (result == null) { // not authenticated
						if (!mode.isSectioning() || CONSTANTS.isAuthenticationRequired() || CONSTANTS.tryAuthenticationWhenGuest()) {
							if (CONSTANTS.allowUserLogin())
								userAuthentication.authenticate();
							else if (!mode.isSectioning() || CONSTANTS.isAuthenticationRequired())
								ToolBox.open(GWT.getHostPageBaseURL() + "login.do?target=" + URL.encodeQueryString(Window.Location.getHref()));
							else
								userAuthentication.authenticated(result);
						} else {
							userAuthentication.authenticated(result);
						}
					} else {
						userAuthentication.authenticated(result);
					}
				}
			});
		
		final AcademicSessionSelector sessionSelector = new AcademicSessionSelector(UniTimePageHeader.getInstance().getRight(), mode);
		
		iSectioningService.getProperties(null, new AsyncCallback<SectioningProperties>() {
			public void onFailure(Throwable caught) {
			}

			public void onSuccess(SectioningProperties result) {
				if (result.isAdmin()) {
					userAuthentication.setAllowLookup(true);
					if (Window.Location.getParameter("session") != null)
						sessionSelector.selectSession(Long.valueOf(Window.Location.getParameter("session")), new AsyncCallback<Boolean>() {
							@Override
							public void onFailure(Throwable caught) {
							}

							@Override
							public void onSuccess(Boolean result) {
								if (Window.Location.getParameter("student") != null)
									UserAuthentication.personFound(Window.Location.getParameter("student"));
							}
						});
				} else {
					userAuthentication.setAllowLookup(false);
				}
			}
		});
		
		UniTimePageHeader.getInstance().getLeft().setVisible(false);
		UniTimePageHeader.getInstance().getLeft().setPreventDefault(true);
		
		final StudentSectioningWidget widget = new StudentSectioningWidget(true, sessionSelector, userAuthentication, mode, true);
		
		initWidget(widget);
		
		UniTimePageHeader.getInstance().getRight().setClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (widget.isChanged() && !Window.confirm(MESSAGES.queryLeaveChanges())) return;
				sessionSelector.selectSession();
			}
		});
		UniTimePageHeader.getInstance().getMiddle().setClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (CONSTANTS.allowUserLogin()) {
					if (widget.isChanged() && !Window.confirm(MESSAGES.queryLeaveChanges())) return;
					if (userAuthentication.isLoggedIn())
						userAuthentication.logOut();
					else
						userAuthentication.authenticate();
				} else if (userAuthentication.isAllowLookup()) {
					userAuthentication.doLookup();
				} else if (userAuthentication.isLoggedIn()) {
					ToolBox.open(GWT.getHostPageBaseURL() + "logOut.do");
				} else {
					ToolBox.open(GWT.getHostPageBaseURL() + "login.do?target=" + URL.encodeQueryString(Window.Location.getHref()));
				}
			}
		});


		userAuthentication.addUserAuthenticatedHandler(new UserAuthentication.UserAuthenticatedHandler() {
			public void onLogIn(UserAuthenticatedEvent event) {
				if (!mode.isSectioning())
					sessionSelector.selectSession(null, false);
				sessionSelector.selectSession();
			}

			public void onLogOut(UserAuthenticatedEvent event) {
				if (!event.isGuest()) {
					widget.clearMessage();
					widget.clear();
					// sessionSelector.selectSession(null);
				}
				userAuthentication.authenticate();
			}
		});
		
		sessionSelector.addAcademicSessionChangeHandler(new AcademicSessionProvider.AcademicSessionChangeHandler() {
			public void onAcademicSessionChange(AcademicSessionProvider.AcademicSessionChangeEvent event) {
				if (event.isChanged()) {
					widget.clearMessage();
					widget.clear();
				}
				widget.checkEligibility(event.getNewAcademicSessionId(), null, false, null);
				userAuthentication.setLookupOptions("mustHaveExternalId,source=students,session=" + event.getNewAcademicSessionId());
			}
		});
		
		if (Window.Location.getParameter("session") == null)
			iSectioningService.lastAcademicSession(mode.isSectioning(), new AsyncCallback<AcademicSessionProvider.AcademicSessionInfo>() {
				public void onFailure(Throwable caught) {
					if (!userAuthentication.isShowing() && !UniTimeFrameDialog.hasDialog())
						sessionSelector.selectSession();
				}
				public void onSuccess(AcademicSessionProvider.AcademicSessionInfo result) {
					sessionSelector.selectSession(result, true);
				}
			});
		
		Window.addWindowClosingHandler(new Window.ClosingHandler() {
			@Override
			public void onWindowClosing(ClosingEvent event) {
				if (widget.isChanged()) {
					if (LoadingWidget.getInstance().isShowing())
						LoadingWidget.getInstance().hide();
					event.setMessage(MESSAGES.queryLeaveChanges());
				}
			}
		});
	}
}
