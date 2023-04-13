package com.copado.copadoplugin.views;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class CopadoPlugin extends ViewPart {

	public static final String ID = "com.copado.copadoplugin.views.CopadoPlugin";
	private static final Path WORKING_DIRECTORY = Paths.get("/Users/vaibhavsapra/Documents/MulesoftDevOrg/MulesoftDevOrg");
	private static final int TIMEOUT_SECONDS = 60;

	@Override
	public void createPartControl(Composite parent) {
		Button switchButton = new Button(parent, SWT.PUSH);
		switchButton.setText("Switch to branch");
		switchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					settingWorktoUserStory();
					MessageDialog.openInformation(getViewSite().getShell(), "Switched to branch",
							"Switched to branch successfully!");
				} catch (IOException ex) {
					ex.printStackTrace();
					MessageDialog.openError(getViewSite().getShell(), "Error",
							"An error occurred while switching to branch.");
				}
			}

			private void settingWorktoUserStory() throws IOException {
				List<String> command = List.of("/usr/local/bin/sfdx", "copado:work:set", "-s", "US-0000029");
				runGitCommand(command, WORKING_DIRECTORY);
			}
		});

		Button addButton = new Button(parent, SWT.PUSH);
		addButton.setText("Add and Commit Changes");
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					addChanges();
					commitChanges();
					MessageDialog.openInformation(getViewSite().getShell(), "Commit Changes",
							"Added and Committed changes successfully!");
				} catch (IOException ex) {
					ex.printStackTrace();
					MessageDialog.openError(getViewSite().getShell(), "Error",
							"An error occurred while adding or committing changes.");
				}
			}

			private void addChanges() throws IOException {
				List<String> command = List.of("git", "add", ".");
				runGitCommand(command, WORKING_DIRECTORY);
			}

			private void commitChanges() throws IOException {
				List<String> command = List.of("git", "commit", "-m", "gitignore changes");
				runGitCommand(command, WORKING_DIRECTORY);
			}
		});

		Button pushChanges = new Button(parent, SWT.PUSH);
		pushChanges.setText("Push Changes");
		pushChanges.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					pushChanges();
					MessageDialog.openInformation(getViewSite().getShell(), "Pushed Changes",
							"Pushed changes successfully!");
				} catch (IOException ex) {
					ex.printStackTrace();
					MessageDialog.openError(getViewSite().getShell(), "Error",
							"An error occurred while pushing changes.");
				}
			}

			private void pushChanges() throws IOException {
				List<String> command = List.of("/usr/local/bin/sfdx", "copado:work:push");
				runGitCommand(command,WORKING_DIRECTORY);
			}
		});
	}

	private static String runGitCommand(List<String> command, Path workingDirectory) throws IOException {
		try {
			File file = File.createTempFile("process", "git");
			ProcessBuilder processBuilder = new ProcessBuilder(command).directory(workingDirectory.toFile())
					.redirectOutput(file);
			Process process = processBuilder.start();

			if (!process.waitFor(getTimeOut(), TimeUnit.SECONDS)) {
				process.destroy();
				throw new IOException("Process timeout after " + getTimeOut() + " seconds");
			} else {
				process.getOutputStream().close();
				int exitCode = process.exitValue();
				if (exitCode != 0) {
					try (BufferedInputStream errorStream = new BufferedInputStream(process.getErrorStream())) {
						throw new IOException("Command execution failed with exit code " + exitCode + ". Error: "
								+ new String(errorStream.readAllBytes()));
					}
				}
			}

			try (FileInputStream inputStream = new FileInputStream(file)) {
				return new String(inputStream.readAllBytes());
			}
		} catch (IOException | InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException(exception);
		} finally {
			System.out.println("Git Operation completed successfully!");
		}
	}

	private static Path getWorkingDirectory() throws IOException {
		List<String> command = List.of("git", "rev-parse", "--show-toplevel");
		Process process = new ProcessBuilder(command).start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line = reader.readLine();
			if (line != null) {
				return Paths.get(line);
			}
		}

		throw new IOException("Git repository not found");
	}

	private static long getTimeOut() {
		return TIMEOUT_SECONDS;
	}

	@Override
	public void setFocus() {
	}
}
