package screach.titanium.gui.servertab;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import screach.titanium.core.NotifyEventType;
import screach.titanium.core.Player;
import screach.titanium.core.server.Server;
import screach.titanium.gui.ServerTabsPane;
import utils.AssetsLoader;
import utils.ErrorUtils;
import utils.Pool;

public abstract class ServerTab extends Tab implements Observer {
	public final static int CONNECTION_ATTEMPS = 5;

	private Server server;

	private ServerTabsPane tabs;

	private Pane connectedPane;
	private Pane notConnectedPane;


	private PlayerListTable connectedPlayerTable;
	private PlayerListTable dcPlayerTable;

	private ObservableList<PlayerView> connectedPlayersList;
	private ObservableList<PlayerView> dcPlayersList;

	private ServerInfoPane serverInfo;
	private Controls controlsPane;

	private Button connectButton; 
	private ProgressIndicator piConnect;
	private Button editButton;
	
	public ServerTab(Server server, ServerTabsPane tabs) {
		super();
		this.server = server;
		this.tabs = tabs;

		this.setClosable(false);

		server.addObserver(this);

		connectedPlayersList = FXCollections.observableArrayList();
		connectedPlayersList.addAll(server.getConnectedPlayers().stream()
				.map(p -> new PlayerView(p, server, tabs.getApplication()))
				.collect(Collectors.toList()));

		dcPlayersList = FXCollections.observableArrayList();
		dcPlayersList.addAll(server.getRecentlyDCPlayers().stream()
				.map(p -> new PlayerView(p, server, tabs.getApplication()))
				.collect(Collectors.toList()));


		setupConnectedPane();
		setupNotConnectedPane();

		this.setContent(notConnectedPane);
		
	}

	public void setupConnectedPane() {
		connectedPlayerTable = new PlayerListTable(connectedPlayersList);
		dcPlayerTable = new PlayerListTable(dcPlayersList);

		connectedPane = new VBox();

		GridPane playersPane = new GridPane();
		playersPane.setPadding(new Insets(10, 0, 10, 0));

		VBox connectePlayersdPane = new VBox();
		VBox dcPlayersPane = new VBox();

		connectePlayersdPane.setPadding(new Insets(0, 5, 0, 0));
		dcPlayersPane.setPadding(new Insets(0, 0, 0, 5));

		Label connectedPlayerLabel = new Label("Connected players");
		connectedPlayerLabel.setPadding(new Insets(0, 0, 5, 0));
		Label dcPlayerLabel = new Label("Disconnected players");
		dcPlayerLabel.setPadding(new Insets(0, 0, 5, 0));



		connectePlayersdPane.getChildren().addAll(connectedPlayerLabel, connectedPlayerTable);
		dcPlayersPane.getChildren().addAll(dcPlayerLabel, dcPlayerTable);

		// Col and row length
		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(50);
		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(50);
		RowConstraints row = new RowConstraints();
		row.setPercentHeight(100);

		playersPane.getColumnConstraints().addAll(column1, column2);
		playersPane.getRowConstraints().add(row);

		playersPane.add(connectePlayersdPane, 0, 0);
		playersPane.add(dcPlayersPane, 1, 0);

		serverInfo = new ServerInfoPane(server);
		controlsPane = new Controls(this);

		connectedPane.setPadding(new Insets(0, 10, 0, 10));
		controlsPane.setPadding(new Insets(0, 0, 10, 0));


		serverInfo.setMinHeight(65);
		serverInfo.setMaxHeight(65);

		controlsPane.setMinHeight(200);
		controlsPane.setMaxHeight(200);

		playersPane.setPrefHeight(1000000);

		connectedPane.getChildren().add(serverInfo);
		connectedPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
		connectedPane.getChildren().add(playersPane);
		connectedPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
		connectedPane.getChildren().addAll(controlsPane);


	}

	public void setupNotConnectedPane() {
		GridPane pane = new GridPane();
		pane.setVgap(15);
		notConnectedPane = pane;
		notConnectedPane.setPadding(new Insets(15, 15, 15, 15));
		
		Label l = new Label("Not connected");
		connectButton = new Button("Connect", AssetsLoader.getIcon("connect.png"));
		editButton = new Button("Edit server informations...", AssetsLoader.getIcon("edit.png"));

		connectButton.setOnAction(this::connectButtonAction);
		editButton.setOnAction(this::editButtonAction);

		piConnect = new ProgressIndicator();
		piConnect.setVisible(false);
		pane.addRow(0, connectButton);
		pane.addRow(1, editButton);
		pane.addRow(2, piConnect);
	}

	public void switchToDisconnected() {
		Platform.runLater(() -> {
			this.setContent(notConnectedPane);

		});
	}

	public void switchToConnected() {
		this.setContent(connectedPane);
		refreshTitle();
	}

	public void connectButtonAction(Event e) {
		
		connectButton.setText("Connection...");
		connectButton.setDisable(true);
		piConnect.setVisible(true);
		editButton.setDisable(true);
		
		
		Pool.submit(() -> {
				Exception lastException = null;
				
				for (int i = 0; i < CONNECTION_ATTEMPS; i++) {
					try {
						connect();
						break;
					} catch (Exception e1) {
						e1.printStackTrace();
						lastException = e1;
					} 
				}
				
				if (!server.isConnected()) {
					switchToDisconnected();
					
					Alert alert = ErrorUtils.newErrorAlert("Server connection error", "Connection to \"" + server + "\" has failed.", lastException.getClass() + " : " + lastException.getMessage());
					alert.show();
				}
				
			Platform.runLater(() -> {
				connectButton.setText("Connect");
				connectButton.setDisable(false);
				piConnect.setVisible(false);
				editButton.setDisable(false);
				tabs.refreshButtonAvailability(this);
			});
		});

	}

	protected abstract void editButtonAction(Event e);



	public void connect() throws Exception {
		server.connect();
		Platform.runLater(() -> {
			switchToConnected();
		});
	}

	public void disconnect() {
		server.disconnect();
		tabs.refreshButtonAvailability(this);
	}

	public Server getServer() {
		return server;
	}

	protected void refreshTitle() {
		Platform.runLater(() -> {
			this.setGraphic(getTitleNodde());
		});
	}

	protected Node getTitleNodde() {
		GridPane result = new GridPane();

		result.setHgap(5);

		result.addRow(0, getConnectionStatusIcon(), new Label(server.getName()));

		return result;
	}

	protected ImageView getConnectionStatusIcon() {
		return (server.isConnected()) ? AssetsLoader.getIcon("connected_icon.png"): AssetsLoader.getIcon("not_connected_icon.png");
	}

	@Override
	public void update(Observable o, Object arg) {

		if (arg instanceof NotifyEventType) {
			NotifyEventType eventType = (NotifyEventType) arg;

			switch (eventType) {
			case CONSOLE_LOG:
				updateConsoleLog(server.getLastLog());
				break;
			case PLAYER_LIST:
				updateConnected(server.getConnectedPlayers());
				updateNotConnected(server.getRecentlyDCPlayers());
				break;
			case MAP_CHANGED:
				updateMaps();
				break;
			case PING:
				updatePing();
				break;
			case VAC:
				updateVac();
				break;
			case DISCONNECT:
				switchToDisconnected();
				refreshTitle();
				break;
			case DISCONNECT_ERROR:
				switchToDisconnectedWithAlert();
				refreshTitle();
				break;
			}


		}

	}

	private void updateVac() {
		//XXX awful way to do it.
		List<PlayerView> tmp = new ArrayList<>(connectedPlayersList);

		connectedPlayersList.clear();
		connectedPlayersList.addAll(tmp);
	}


	private void updateConnected(List<Player> players) {
		List<PlayerView> toRemove = new ArrayList<PlayerView>();

		// TODO user Player instead of PlayerView for contains().

		connectedPlayersList.forEach(p -> {
			if (!players.contains(p.getPlayer())) 
				toRemove.add(p);
		});

		connectedPlayersList.removeAll(toRemove);

		players.forEach(p -> {
			if (!connectedPlayersList.contains(p))
				connectedPlayersList.add(new PlayerView(p, server, tabs.getApplication()));
		});

		serverInfo.refreshPlayerCount();

	}

	private void updateNotConnected(List<Player> players) {
		players.forEach(p -> {
			if (!dcPlayersList.contains(p))
				dcPlayersList.add(new PlayerView(p, server, tabs.getApplication()));
		});
	}

	private void updateConsoleLog(String log) {
		controlsPane.addLog(log);
	}

	private void updateMaps() {
		serverInfo.refreshMaps();
	}

	private void updatePing() {
		serverInfo.refreshPing();
	}

	public void switchToDisconnectedWithAlert() {
		switchToDisconnected();
		Platform.runLater(() -> {
			Alert a = ErrorUtils.newErrorAlert("Disconnected from " + server.getName(), "Server connection lost.","");
			a.show();
		});
	}
}
