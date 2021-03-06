package screach.titanium.gui.dialogs;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import screach.titanium.core.server.LocalServer;
import screach.titanium.gui.dialogs.listeners.RequieredListener;

public class EditServerDialog extends Dialog<LocalServer>{
	public EditServerDialog(LocalServer server) {
		super();
		// Create the custom dialog.
		this.setTitle("Edit server");
		this.setHeaderText("Server informations");


		// Set the button types.
		ButtonType addButtonType = new ButtonType("Edit server", ButtonData.OK_DONE);
		this.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

		// Create the username and password labels and fields.
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		System.out.println("server : " + server);
		TextField serverName = new TextField(server.getName());
		TextField address = new TextField(server.getAddress());
		TextField port = new TextField(server.getPort() + "");
		PasswordField password = new PasswordField();
		password.setText(server.getPassword());


		grid.add(new Label("Server name"), 0, 0);
		grid.add(serverName, 1, 0);
		grid.add(new Label("Address"), 0, 1);
		grid.add(address, 1, 1);
		grid.add(new Label("Port"), 0, 2);
		grid.add(port, 1, 2);
		grid.add(new Label("Password"), 0, 3);
		grid.add(password, 1, 3);


		port.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					port.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});

		Node addButton = this.getDialogPane().lookupButton(addButtonType);
		
		
		
		// Verify required inputs
		// TODO not working
		serverName.textProperty().addListener(new RequieredListener(addButton));
		address.textProperty().addListener(new RequieredListener(addButton));
		port.textProperty().addListener(new RequieredListener(addButton));
		password.textProperty().addListener(new RequieredListener(addButton));

		
		this.getDialogPane().setContent(grid);

		this.setResultConverter(dialogButton -> {
			try {
				if (dialogButton == addButtonType) {
					return new LocalServer(serverName.getText(), address.getText(), Integer.parseInt(port.getText()), password.getText());
				}
			} catch (NumberFormatException e) {
				return null;
			}
			return null;
		});

	}



}
