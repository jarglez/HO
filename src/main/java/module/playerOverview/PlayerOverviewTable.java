package module.playerOverview;

import core.db.DBManager;
import core.gui.HOMainFrame;
import core.gui.RefreshManager;
import core.gui.comp.renderer.HODefaultTableCellRenderer;
import core.gui.comp.table.TableSorter;
import core.gui.comp.table.ToolTipHeader;
import core.gui.comp.table.UserColumn;
import core.gui.model.PlayerOverviewModel;
import core.gui.model.UserColumnController;
import core.gui.model.UserColumnFactory;
import core.model.HOVerwaltung;
import core.model.UserParameter;
import core.model.match.MatchKurzInfo;
import core.model.player.Player;
import core.net.HattrickLink;
import core.util.Helper;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * The main player table.
 * 
 * @author Thorsten Dietz
 */
public class PlayerOverviewTable extends JTable implements core.gui.Refreshable {

	private static final long serialVersionUID = -6074136156090331418L;
	private PlayerOverviewModel tableModel;
	private TableSorter tableSorter;

	public PlayerOverviewTable() {
		super();
		initModel();
		setDefaultRenderer(Object.class, new HODefaultTableCellRenderer());
		setSelectionBackground(HODefaultTableCellRenderer.SELECTION_BG);
		RefreshManager.instance().registerRefreshable(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int rowindex = getSelectedRow();
				if (rowindex >= 0){
					// Last match column
					String columnName = tableSorter.getColumnName(columnAtPoint(e.getPoint()));
					String lastMatchRating = (HOVerwaltung.instance().getLanguageString("LastMatchRating"));

					Player player = tableSorter.getSpieler(rowindex);
					if(player!=null){
						if(columnName.equalsIgnoreCase(lastMatchRating)){
							if(e.isShiftDown()){
								int matchId = player.getLastMatchId();
								MatchKurzInfo info = DBManager.instance().getMatchesKurzInfoByMatchID(matchId);
								HattrickLink.showMatch(matchId + "", info.getMatchTyp().isOfficial());
							}else if(e.getClickCount()==2) {
								HOMainFrame.instance().showMatch(player.getLastMatchId());
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Breite der BestPosSpalte zurückgeben
	 */
	public final int getBestPosWidth() {
		return getColumnModel().getColumn(
				getColumnModel().getColumnIndex(tableModel.getPositionInArray(UserColumnFactory.BEST_POSITION)))
				.getWidth();
	}

	public final TableSorter getSorter() {
		return tableSorter;
	}

	/**
	 * @return int[spaltenanzahl][2] mit 0=ModelIndex und 1=ViewIndex
	 */
	public final int[][] getSpaltenreihenfolge() {
		final int[][] reihenfolge = new int[tableModel.getColumnCount()][2];

		for (int i = 0; i < tableModel.getColumnCount(); i++) {
			// Modelindex
			reihenfolge[i][0] = i;
			// ViewIndex
			reihenfolge[i][1] = convertColumnIndexToView(i);
		}
		return reihenfolge;
	}

	public final void saveColumnOrder() {
		UserColumn[] columns = tableModel.getDisplayedColumns();
		TableColumnModel tableColumnModel = getColumnModel();
		for (int i = 0; i < columns.length; i++) {
			columns[i].setIndex(convertColumnIndexToView(i));
			columns[i].setPreferredWidth(tableColumnModel.getColumn(convertColumnIndexToView(i)).getWidth());
		}
		tableModel.setCurrentValueToColumns(columns);
		DBManager.instance().saveHOColumnModel(tableModel);
	}

	public final void setSpieler(int spielerid) {
		final int index = tableSorter.getRow4Spieler(spielerid);

		if (index >= 0) {
			this.setRowSelectionInterval(index, index);
		}
	}

	@Override
	public final void reInit() {
		initModel();
		repaint();
	}

	public final void reInitModel() {
		((PlayerOverviewModel) getSorter().getModel()).reInitData();
	}

	public final void reInitModelHRFVergleich() {
		((PlayerOverviewModel) getSorter().getModel()).reInitDataHRFVergleich();
	}

	@Override
	public final void refresh() {
		reInitModel();
		repaint();
	}

	public final void refreshHRFVergleich() {
		reInitModelHRFVergleich();
		repaint();
	}

	/**
	 * Gibt die Spalte für die Sortierung zurück
	 */
	private int getSortSpalte() {
		return switch (UserParameter.instance().standardsortierung) {
			case UserParameter.SORT_NAME -> tableModel.getPositionInArray(UserColumnFactory.NAME);
			case UserParameter.SORT_AUFGESTELLT -> tableModel.getPositionInArray(UserColumnFactory.LINUP);
			case UserParameter.SORT_GRUPPE -> tableModel.getPositionInArray(UserColumnFactory.GROUP);
			case UserParameter.SORT_BEWERTUNG -> tableModel.getPositionInArray(UserColumnFactory.RATING);
			default -> tableModel.getPositionInArray(UserColumnFactory.BEST_POSITION);
		};
	}

	/**
	 * Initialisiert das Model
	 */
	private void initModel() {
		setOpaque(false);

		if (tableModel == null) {
			tableModel = UserColumnController.instance().getPlayerOverviewModel();
			tableModel.setValues(HOVerwaltung.instance().getModel().getCurrentPlayers());
			tableSorter = new TableSorter(tableModel,
					tableModel.getPositionInArray(UserColumnFactory.ID),
					getSortSpalte(),
					tableModel.getPositionInArray(UserColumnFactory.NAME));

			ToolTipHeader header = new ToolTipHeader(getColumnModel());
			header.setToolTipStrings(tableModel.getTooltips());
			header.setToolTipText("");
			setTableHeader(header);

			setModel(tableSorter);

			TableColumnModel tableColumnModel = getColumnModel();
			for (int i = 0; i < tableModel.getColumnCount(); i++) {
				tableColumnModel.getColumn(i).setIdentifier(i);
			}

			int[][] targetColumn = tableModel.getColumnOrder();

			// Reihenfolge -> nach [][1] sortieren
			targetColumn = Helper.sortintArray(targetColumn, 1);

			if (targetColumn != null) {
				for (int i = 0; i < targetColumn.length; i++) {
					this.moveColumn(getColumnModel().getColumnIndex(targetColumn[i][0]), targetColumn[i][1]);
				}
			}

			tableSorter.addMouseListenerToHeaderInTable(this);
			tableModel.setColumnsSize(getColumnModel());
		} else {
			// Werte neu setzen
			tableModel.setValues(HOVerwaltung.instance().getModel().getCurrentPlayers());
			tableSorter.reallocateIndexes();
		}

		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setRowSelectionAllowed(true);
		tableSorter.initsort();
	}
}
