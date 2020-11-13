package com.web.gwt.client.view.widgets;

import com.baselet.control.enums.Program;
import com.baselet.element.interfaces.Diagram;
import com.baselet.gwt.client.element.DiagramXmlParser;
import com.baselet.gwt.client.view.CanvasUtils;
import com.baselet.gwt.client.view.widgets.DownloadPopupPanel;
import com.baselet.gwt.client.view.widgets.FilenameAndScaleHolder;
import com.baselet.gwt.client.view.widgets.InputEvent;
import com.baselet.gwt.client.view.widgets.InputHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;

import java.util.Date;

public class WebDownloadPopupPanel extends DownloadPopupPanel {

	private static DateTimeFormat DTF = DateTimeFormat.getFormat("yyyy-MM-dd HH-mm-ss");

	private Timer timerRename;
	private Timer timer;

	@Override
	public void prepare(FilenameAndScaleHolder filenameAndScaleHolder) {
		setHeader("Export Diagram");
		FlowPanel panel = new FlowPanel();
		HTML w = new HTML("Optionally set a filename");
		panel.add(w);
		final TextBox textBox = new TextBox();
		panel.add(textBox);

		// handle scaling
		HTML scaleHtml = new HTML("Set scaling of Image file:");
		panel.add(scaleHtml);
		final TextBox scaleBox = new TextBox();
		panel.add(scaleBox);
		scaleBox.setValue("1.0");
		filenameAndScaleHolder.setScaling(1d);

		textBox.setValue(filenameAndScaleHolder.getFilename());

		Diagram diagram = drawPanelDiagram.getDiagram();
		String pngUrl = CanvasUtils.createPngCanvasDataUrl(diagram);
		String pdfUrl = CanvasUtils.createPdfCanvasDataUrl(diagram, 1.0);
		String uxfUrl = "data:text/xml;charset=utf-8," + DiagramXmlParser.diagramToXml(true, true, diagram);

		final HTML downloadLinkHtml = new HTML(createDownloadLinks(uxfUrl, pngUrl, pdfUrl, filenameAndScaleHolder.getFilename()));
		panel.add(downloadLinkHtml);
		panel.add(new HTML("<div style=\"color:gray;\">To change the target directory</div><div style=\"color:gray;\">use \"Right click -&gt; Save as\"</div>"));
		setWidget(panel);
		// listen to all input events from the browser (http://stackoverflow.com/a/43089693)
		textBox.addDomHandler(new InputHandler() {
			@Override
			public void onInput(InputEvent event) {
				cancelTimer();
				downloadLinkHtml.setHTML("");

				timer = new Timer() {
					@Override
					public void run() {
						filenameAndScaleHolder.setFilename(textBox.getText());
						String renamedPngUrl = CanvasUtils.createPngCanvasDataUrl(diagram, filenameAndScaleHolder.getScaling());
						String renamedPdfUrl = CanvasUtils.createPdfCanvasDataUrl(diagram, filenameAndScaleHolder.getScaling());
						downloadLinkHtml.setHTML(createDownloadLinks(uxfUrl, renamedPngUrl, renamedPdfUrl, filenameAndScaleHolder.getFilename()));
					}
				};
				timer.schedule(2000);
			}
		}, InputEvent.getType());
		// renew download links when scaling is changed
		scaleBox.addDomHandler(new InputHandler() {
			@Override
			public void onInput(InputEvent event) {
				try {
					cancelTimer();
					downloadLinkHtml.setHTML("");
					double scalingValue = Double.parseDouble(scaleBox.getValue());
					if (scalingValue <= 0)
						throw new Exception();

					timer = new Timer() {
						@Override
						public void run() {
							filenameAndScaleHolder.setScaling(scalingValue);
							String scaledPngUrl = CanvasUtils.createPngCanvasDataUrl(diagram, filenameAndScaleHolder.getScaling());
							downloadLinkHtml.setHTML(createDownloadLinks(uxfUrl, scaledPngUrl, pdfUrl, filenameAndScaleHolder.getFilename()));
						}
					};
					timer.schedule(2000);
				} catch (Exception e) {
					cancelTimer();
					downloadLinkHtml.setHTML("");
					// wrong scaling value, just default to standard
					timer = new Timer() {
						@Override
						public void run() {
							filenameAndScaleHolder.setFilename(textBox.getText());
							filenameAndScaleHolder.setScaling(1.0d);
							downloadLinkHtml.setHTML(createDownloadLinks(uxfUrl, pngUrl, pdfUrl, filenameAndScaleHolder.getFilename()));
						}
					};
					timer.schedule(2000);
				}
			}
		}, InputEvent.getType());
	}

	private String createDownloadLinks(String uxfUrl, String pngUrl, String pdfUrl, String filename) {
		if (filename.isEmpty()) {
			filename = "Diagram " + DTF.format(new Date());
		}
		return "<p class=\"exportLink\">" + link(uxfUrl, filename + "." + Program.getInstance().getExtension()) + "Save Diagram File</a></p>" + "<p class=\"exportLink\">" + link(pngUrl, filename + ".png") + "Save PDF File</a></p>" + "<p class=\"exportLink\">" + link(pdfUrl, filename + ".pdf") + "Save PDF File</a></p>";
	}

	private String link(String uxfUrl, String filename) {
		return "<a download=\"" + filename + "\" href='" + uxfUrl.replace("'", "&apos;") + "'>"; // apostrophes in datauris must be escaped because it's the closing sign fore the href
	}

	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
		}
		if (timerRename != null) {
			timerRename.cancel();
		}
	}
}
