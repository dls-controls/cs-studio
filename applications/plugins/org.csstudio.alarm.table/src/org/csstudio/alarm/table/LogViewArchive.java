/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

package org.csstudio.alarm.table;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import org.csstudio.alarm.dbaccess.ArchiveDBAccess;
import org.csstudio.alarm.dbaccess.FilterSetting;
import org.csstudio.alarm.dbaccess.archivedb.ILogMessageArchiveAccess;
import org.csstudio.alarm.table.dataModel.JMSMessage;
import org.csstudio.alarm.table.dataModel.JMSMessageList;
import org.csstudio.alarm.table.expertSearch.ExpertSearchDialog;
import org.csstudio.alarm.table.internal.localization.Messages;
import org.csstudio.alarm.table.logTable.JMSLogTableViewer;
import org.csstudio.alarm.table.preferences.LogArchiveViewerPreferenceConstants;
import org.csstudio.alarm.table.preferences.LogViewerPreferenceConstants;
import org.csstudio.alarm.table.readDB.DBAnswer;
import org.csstudio.alarm.table.readDB.ReadDBJob;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.util.time.StartEndTimeParser;
import org.csstudio.util.time.swt.StartEndDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

/**
 * Simple view more like console, used to write log messages.
 * 
 * @author jhatje
 * @author $Author$
 * @version $Revision$
 * @since 19.07.2007
 */
public class LogViewArchive extends ViewPart implements Observer {

    /**  The Id of this Object. */
	public static final String ID = LogViewArchive.class.getName();

    /** The Parent Shell. */
	private Shell _parentShell = null;

    /** The JMS message list. */
	private JMSMessageList _jmsMessageList = null;

    /** The JMS Logtable Viewer. */
	private JMSLogTableViewer _jmsLogTableViewer = null;

    /** An Array whit the name of the Columns. */
	private String[] _columnNames;

    /** Textfield witch contain the "from time". */
	private Text _timeFrom;
    /** Textfield witch contain the "to time". */
	private Text _timeTo;

    /** The selectet "from time". */ 
    private ITimestamp _fromTime;
    /** The selectet "to time". */
    private ITimestamp _toTime;

    /** The column property change listener. */
	private ColumnPropertyChangeListener _columnPropertyChangeListener;
    
    /** The default / last filter. */
    private String _filter= ""; //$NON-NLS-1$

    /** 
     * The Answer from the DB.
     */
    private DBAnswer _dbAnswer = null;

    /** The Display. */
	private Display _disp;

	/** The count of results. */
    private Label _countLabel;

    
    public LogViewArchive() {
    	super();
    	_dbAnswer = new DBAnswer();
    	_dbAnswer.addObserver(this);
    }
    
    /** {@inheritDoc} */
	public final void createPartControl(final Composite parent) {

		_disp = parent.getDisplay();

		_columnNames = JmsLogsPlugin.getDefault().getPluginPreferences()
				.getString(LogArchiveViewerPreferenceConstants.P_STRINGArch)
				.split(";"); //$NON-NLS-1$
		_jmsMessageList = new JMSMessageList(_columnNames);

		_parentShell = parent.getShell();

		GridLayout grid = new GridLayout();
		grid.numColumns = 1;
		parent.setLayout(grid);
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1));
		comp.setLayout(new GridLayout(4, false));

		Group buttons = new Group(comp, SWT.LINE_SOLID);
		buttons.setText(Messages.getString("LogViewArchive_period")); //$NON-NLS-1$
		buttons.setLayout(new GridLayout(5, true));
		GridData gd = new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1);
		gd.minimumHeight = 60;
		gd.minimumWidth = 300;
		buttons.setLayoutData(gd);

		create24hButton(buttons);
		create72hButton(buttons);
		createWeekButton(buttons);
		createFlexButton(buttons);
		createSearchButton(buttons);


		Group from = new Group(comp, SWT.LINE_SOLID);
		from.setText(Messages.getString("LogViewArchive_from")); //$NON-NLS-1$
	    gd = new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1);
        gd.minimumHeight = 60;
        gd.minimumWidth = 150;
		from.setLayoutData(gd);
		from.setLayout(new GridLayout(1, true));

		_timeFrom = new Text(from, SWT.SINGLE);
		_timeFrom.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false,1,1));

		_timeFrom.setEditable(false);
		_timeFrom.setText("                            "); //$NON-NLS-1$
		Group to = new Group(comp, SWT.LINE_SOLID);
		to.setText(Messages.getString("LogViewArchive_to")); //$NON-NLS-1$
		gd = new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1);
        gd.minimumHeight = 60;
        gd.minimumWidth = 150;
		to.setLayoutData(gd);
		to.setLayout(new GridLayout(1, true));

		_timeTo = new Text(to, SWT.SINGLE);
		_timeTo.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false,1,1));
		_timeTo.setEditable(false);
//		timeTo.setText("                              ");

        Group count = new Group(comp, SWT.LINE_SOLID);
        count.setText(Messages.getString("LogViewArchive_count")); //$NON-NLS-1$
        count.setLayout(new GridLayout(1, true));
        gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        gd.minimumHeight = 60;
        gd.minimumWidth = 75;
        count.setLayoutData(gd);

        _countLabel = new Label(count,SWT.RIGHT);
        gd = new GridData(SWT.FILL,SWT.CENTER,true, false,1,1);
        _countLabel.setLayoutData(gd);
        _countLabel.setText("0"); //$NON-NLS-1$
        
		
		_jmsLogTableViewer = new JMSLogTableViewer(parent, getSite(), _columnNames, _jmsMessageList, 3,SWT.SINGLE | SWT.FULL_SELECTION);
		_jmsLogTableViewer.setAlarmSorting(false);
		parent.pack();
		
		_columnPropertyChangeListener = new ColumnPropertyChangeListener(
				LogArchiveViewerPreferenceConstants.P_STRINGArch,
				_jmsLogTableViewer);
		
		JmsLogsPlugin.getDefault().getPluginPreferences()
				.addPropertyChangeListener(_columnPropertyChangeListener);
		
	}

    /** 
     * Create a Button to selet the last 24 hour.
     * @param comp the parent Composite for the Button. 
     */
    private void create24hButton(final Composite comp) {
        Button b24hSearch = new Button(comp, SWT.PUSH);
        b24hSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,
                1, 1));
        b24hSearch.setText(Messages.getString("LogViewArchive_day")); //$NON-NLS-1$

        b24hSearch.addSelectionListener(new SelectionAdapter() {
 
			public void widgetSelected(final SelectionEvent e) {
//                ILogMessageArchiveAccess adba = new ArchiveDBAccess();
                GregorianCalendar to = new GregorianCalendar();
                GregorianCalendar from = (GregorianCalendar) to.clone();
                from.add(GregorianCalendar.HOUR_OF_DAY, -24);
                showNewTime(from, to);
                ReadDBJob readDB = new ReadDBJob("DB Reader",  //$NON-NLS-1$
                		LogViewArchive.this._dbAnswer, from, to);
                readDB.schedule();
            }
        });

    }

    /** 
     * Create the a Button to selet the last 72 hour.
     * @param comp the parent {@link Composite} for the Button. 
     */
	private void create72hButton(final Composite comp) {
		Button b72hSearch = new Button(comp, SWT.PUSH);
		b72hSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,
				1, 1));
		b72hSearch.setText(Messages.getString("LogViewArchive_3days")); //$NON-NLS-1$

		b72hSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
//				ILogMessageArchiveAccess adba = new ArchiveDBAccess();
				GregorianCalendar to = new GregorianCalendar();
				GregorianCalendar from = (GregorianCalendar) to.clone();
				from.add(GregorianCalendar.HOUR_OF_DAY, -72);
				showNewTime(from, to);
                ReadDBJob readDB = new ReadDBJob("DB Reader",  //$NON-NLS-1$
                		LogViewArchive.this._dbAnswer, from, to);
                readDB.schedule();
			}
		});
	}
    
    /** 
     * Create a Button to selet the last week.
     * @param comp the parent Composite for the Button. 
     */
	private void createWeekButton(final Composite comp) {
		Button b168hSearch = new Button(comp, SWT.PUSH);
		b168hSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,1, 1));
		b168hSearch.setText(Messages.getString("LogViewArchive_week")); //$NON-NLS-1$

		b168hSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				GregorianCalendar to = new GregorianCalendar();
				GregorianCalendar from = (GregorianCalendar) to.clone();
				from.add(GregorianCalendar.HOUR_OF_DAY, -168);
				showNewTime(from, to);
                ReadDBJob readDB = new ReadDBJob("DB Reader",  //$NON-NLS-1$
                		LogViewArchive.this._dbAnswer, from, to);
                readDB.schedule();
			}
		});

	}

    /** 
     * Create a Button that open a dialog to select required period.
     * @param comp the parent Composite for the Button. 
     */
	private void createFlexButton(final Composite comp) {
		Button bFlexSearch = new Button(comp, SWT.PUSH);
		bFlexSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,
				1, 1));
		bFlexSearch.setText(Messages.getString("LogViewArchive_user")); //$NON-NLS-1$

		bFlexSearch.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				StartEndDialog dlg = new StartEndDialog(_parentShell);
				if (dlg.open() == StartEndDialog.OK) {
					String lowString = dlg.getStartSpecification();
					String highString = dlg.getEndSpecification();
					try {
						StartEndTimeParser parser = new StartEndTimeParser(lowString, highString);
						Calendar from = parser.getStart();
						Calendar to = parser.getEnd();
//    					ILogMessageArchiveAccess adba = new ArchiveDBAccess();
    					showNewTime(from, to);
//    					ArrayList<HashMap<String, String>> am = adba.getLogMessages(from, to);
//    					_jmsMessageList.clearList();
//    					_jmsLogTableViewer.refresh();
//    					_jmsMessageList.addJMSMessageList(am);

    	                ReadDBJob readDB = new ReadDBJob("DB Reader",  //$NON-NLS-1$
    	                		LogViewArchive.this._dbAnswer, from, to);
//    	                ArrayList<HashMap<String, String>> am = adba.getLogMessages(
//    	                        from, to);
    	                readDB.schedule();

					} catch (Exception e1) {
						// TODO Auto-generated catch block
						JmsLogsPlugin.logInfo(e1.getMessage());
					}

				}
			}
		});

	}
    /** 
     * Create a Button that open a dialog to select required period and define filters.
     * @param comp the parent Composite for the Button. 
     */
    private void createSearchButton(final Composite comp) {
		Button bSearch = new Button(comp, SWT.PUSH);
		bSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,
				1, 1));
		bSearch.setText(Messages.getString("LogViewArchive_expert")); //$NON-NLS-1$

		bSearch.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent e) {
				if(_fromTime==null){
                    ITimestamp now = TimestampFactory.now();
				    _fromTime = TimestampFactory.createTimestamp(now.seconds()-(24*60*60), now.nanoseconds()); //new Timestamp(fromDate.getTime()/1000);
                }
                if(_toTime==null){
                    _toTime = TimestampFactory.now();
                }

				ExpertSearchDialog dlg = new ExpertSearchDialog(_parentShell, _fromTime, _toTime, _filter);

				GregorianCalendar to = new GregorianCalendar();
				GregorianCalendar from = (GregorianCalendar) to.clone();
				if (dlg.open() == ExpertSearchDialog.OK) {
                    _fromTime = dlg.getStart();
                    _toTime = dlg.getEnd();
					double low = _fromTime.toDouble();
					double high = _toTime.toDouble();
					if (low < high) {
						from.setTimeInMillis((long) low * 1000);
						to.setTimeInMillis((long) high * 1000);
					} else {
						from.setTimeInMillis((long) high * 1000);
						to.setTimeInMillis((long) low * 1000);
					}
					showNewTime(from, to);

					_filter = dlg.getFilterString();
					
				}
//				ILogMessageArchiveAccess adba = new ArchiveDBAccess();
//				from.add(GregorianCalendar.HOUR, -504);
				showNewTime(from, to);
//				ArrayList<HashMap<String, String>> am;
//				if(_filter.trim().length()>0){
//					am = adba.getLogMessages(from, to, _filter);
//				}else{
//					am = adba.getLogMessages(from, to);
//				}
//				_jmsMessageList.clearList();
//				_jmsLogTableViewer.refresh();
//				_jmsMessageList.addJMSMessageList(am);
                ReadDBJob readDB = new ReadDBJob("DB Reader",  //$NON-NLS-1$
                		LogViewArchive.this._dbAnswer, from, to, _filter, 
                		dlg.get_filterConditions());
                readDB.schedule();

			}
		});
	}

    
    public void readDBFromExternalCall(IProcessVariable pv) {
    	GregorianCalendar from = new GregorianCalendar();
    	GregorianCalendar to = new GregorianCalendar();
    	from.setTimeInMillis(to.getTimeInMillis() - 1000*60*60*24);
    	System.out.println("from: " + from.toString() + "   to: " + to.toString()); //$NON-NLS-1$ //$NON-NLS-2$
    	_filter = "AND ( (lower(aam.PROPERTY) like lower('NAME') AND lower(aam.VALUE) like lower('" + //$NON-NLS-1$
    	pv.getName() + "')))"; //$NON-NLS-1$
    	ArrayList<FilterSetting> filterSettings = new ArrayList<FilterSetting>();
    	filterSettings.add(new FilterSetting("name", pv.getName(), "END"));
        ReadDBJob readDB = new ReadDBJob("DB Reader",  //$NON-NLS-1$
        		LogViewArchive.this._dbAnswer, from, to, _filter, filterSettings);
        readDB.schedule();

    }
    
	/**
     * Set the two times from, to .
     * @param from the from a selectet.
     * @param to the to a selectet.
	 */
	private void showNewTime(final Calendar from, final Calendar to) {
		SimpleDateFormat sdf = new SimpleDateFormat();
		try{
			sdf.applyPattern(JmsLogsPlugin.getDefault().getPreferenceStore().getString(LogArchiveViewerPreferenceConstants.DATE_FORMAT));
		}catch(Exception e){
			sdf.applyPattern(JmsLogsPlugin.getDefault().getPreferenceStore().getDefaultString(LogArchiveViewerPreferenceConstants.DATE_FORMAT));
			JmsLogsPlugin.getDefault().getPreferenceStore().setToDefault(LogArchiveViewerPreferenceConstants.DATE_FORMAT);
		}
		_timeFrom.setText(sdf.format(from.getTime()));
        _fromTime = TimestampFactory.fromCalendar(from);

        _timeTo.setText(sdf.format(to.getTime()));
        _toTime = TimestampFactory.fromCalendar(to);
        // redraw
        _timeFrom.getParent().getParent().redraw();
	}

	/** {@inheritDoc} */
    @Override
	public void setFocus() { }
    /** {@inheritDoc} */
    @Override
    public final void dispose() {
		super.dispose();
		ArchiveDBAccess.getInstance().close();
		JmsLogsPlugin.getDefault().getPluginPreferences()
				.removePropertyChangeListener(_columnPropertyChangeListener);
	}

    /** @return get the from Time. */
	public final Date getFromTime(){
		return _fromTime.toCalendar().getTime();

	}
    /** @return get the to Time. */
	public final Date getToTime(){
		return _toTime.toCalendar().getTime();

	}

	/**
	 * When dispose store the width for each column.
	 */
	public void saveColumn(){
		int[] width = _jmsLogTableViewer.getColumnWidth();
		String newPreferenceColumnString=""; //$NON-NLS-1$
		String[] columns = JmsLogsPlugin.getDefault().getPluginPreferences().getString(LogArchiveViewerPreferenceConstants.P_STRINGArch).split(";"); //$NON-NLS-1$
		if(width.length!=columns.length){
			return;
		}
		for (int i = 0; i < columns.length; i++) {
			newPreferenceColumnString = newPreferenceColumnString.concat(columns[i].split(",")[0]+","+width[i]+";"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		newPreferenceColumnString = newPreferenceColumnString.substring(0,newPreferenceColumnString.length()-1);
		IPreferenceStore store = JmsLogsPlugin.getDefault().getPreferenceStore();
		store.setValue(LogArchiveViewerPreferenceConstants.P_STRINGArch, newPreferenceColumnString);
		if(store.needsSaving()){
			JmsLogsPlugin.getDefault().savePluginPreferences();
		}
	}


	public void update(Observable arg0, Object arg1) {
		_disp.syncExec(new Runnable() {
			public void run() {
                _jmsMessageList.clearList();
                _jmsLogTableViewer.refresh();
                ArrayList<HashMap<String, String>> answer = _dbAnswer.getDBAnswer();
                int size = answer.size();
                if (size > 0) {
                    _countLabel.setText(Integer.toString(size));
                	_jmsMessageList.addJMSMessageList(answer);
                } else {
            		String[] propertyNames = JmsLogsPlugin.getDefault().getPluginPreferences().
        			getString(LogViewerPreferenceConstants.P_STRING).split(";"); //$NON-NLS-1$

            		JMSMessage jmsMessage = new JMSMessage(propertyNames);
            		String firstColumnName = _columnNames[0];
            		jmsMessage.setProperty(firstColumnName, Messages.LogViewArchive_NoMessageInDB);
                	_jmsMessageList.addJMSMessage(jmsMessage);
                }
               }
		});
	}

}
