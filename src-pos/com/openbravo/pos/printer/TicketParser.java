//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.
package com.openbravo.pos.printer;

import com.openbravo.data.loader.LocalRes;
import java.io.*;
import java.awt.image.BufferedImage;
import java.applet.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import com.openbravo.pos.forms.DataLogicSystem;

public class TicketParser extends DefaultHandler {

    private static SAXParser m_sp = null;

    private final DeviceTicket m_printer;
    private final DataLogicSystem m_system;

    private StringBuffer text;

    private String bctype;
    private String bcposition;
    private int m_iTextAlign;
    private int m_iTextLength;
    private int m_iTextStyle;

    private StringBuffer m_sVisorLine;
    private int m_iVisorAnimation;
    private String m_sVisorLine1;
    private String m_sVisorLine2;

    private double m_dValue1;
    private double m_dValue2;
    private int attribute3;

    private int m_iOutputType;
    private static final int OUTPUT_NONE = 0;
    private static final int OUTPUT_DISPLAY = 1;
    private static final int OUTPUT_TICKET = 2;
    private static final int OUTPUT_FISCAL = 3;
    private DevicePrinter m_oOutputPrinter;

    /**
     * Creates a new instance of TicketParser
     *
     * @param printer
     * @param system
     */
    public TicketParser(DeviceTicket printer, DataLogicSystem system) {
        m_printer = printer;
        m_system = system;
    }

    public void printTicket(String sIn) throws TicketPrinterException {
        printTicket(new StringReader(sIn));
    }

    public void printTicket(Reader in) throws TicketPrinterException {

        try {

            if (m_sp == null) {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                m_sp = spf.newSAXParser();
            }
            m_sp.parse(new InputSource(in), this);

        } catch (ParserConfigurationException ePC) {
            throw new TicketPrinterException(LocalRes.getIntString("exception.parserconfig"), ePC);
        } catch (SAXException eSAX) {
            throw new TicketPrinterException(LocalRes.getIntString("exception.xmlfile"), eSAX);
        } catch (IOException eIO) {
            throw new TicketPrinterException(LocalRes.getIntString("exception.iofile"), eIO);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        // inicalizo las variables pertinentes
        text = null;
        bctype = null;
        bcposition = null;
        m_sVisorLine = null;
        m_iVisorAnimation = DeviceDisplayBase.ANIMATION_NULL;
        m_sVisorLine1 = null;
        m_sVisorLine2 = null;
        m_iOutputType = OUTPUT_NONE;
        m_oOutputPrinter = null;
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        switch (m_iOutputType) {
            case OUTPUT_NONE -> {
                if (null != qName) {
                    switch (qName) {
                        case "opendrawer" ->
                            m_printer.getDevicePrinter(readString(attributes.getValue("printer"), "1")).openDrawer();
                        case "play" ->
                            text = new StringBuffer();
                        case "ticket" -> {
                            m_iOutputType = OUTPUT_TICKET;
                            m_oOutputPrinter = m_printer.getDevicePrinter(readString(attributes.getValue("printer"), "1"));
                            m_oOutputPrinter.beginReceipt();
                        }
                        case "display" -> {
                            m_iOutputType = OUTPUT_DISPLAY;
                            String animation = attributes.getValue("animation");
                            if (null == animation) { // "none"
                                m_iVisorAnimation = DeviceDisplayBase.ANIMATION_NULL;
                            } else {
                                m_iVisorAnimation = switch (animation) {
                                    case "scroll" ->
                                        DeviceDisplayBase.ANIMATION_SCROLL;
                                    case "flyer" ->
                                        DeviceDisplayBase.ANIMATION_FLYER;
                                    case "blink" ->
                                        DeviceDisplayBase.ANIMATION_BLINK;
                                    case "curtain" ->
                                        DeviceDisplayBase.ANIMATION_CURTAIN;
                                    default ->
                                        DeviceDisplayBase.ANIMATION_NULL;
                                }; // "none"
                            }
                            m_sVisorLine1 = null;
                            m_sVisorLine2 = null;
                            m_oOutputPrinter = null;
                        }
                        case "fiscalreceipt" -> {
                            m_iOutputType = OUTPUT_FISCAL;
                            m_printer.getFiscalPrinter().beginReceipt();
                        }
                        case "fiscalzreport" ->
                            m_printer.getFiscalPrinter().printZReport();
                        case "fiscalxreport" ->
                            m_printer.getFiscalPrinter().printXReport();
                        default -> {
                        }
                    }
                }
            }

            case OUTPUT_TICKET -> {
                if (null != qName) {
                    switch (qName) {
                        case "image" ->
                            text = new StringBuffer();
                        case "barcode" -> {
                            text = new StringBuffer();
                            bctype = attributes.getValue("type");
                            bcposition = attributes.getValue("position");
                        }
                        case "line" ->
                            m_oOutputPrinter.beginLine(parseInt(attributes.getValue("size"), DevicePrinter.SIZE_0));
                        case "text" -> {
                            text = new StringBuffer();
                            m_iTextStyle = ("true".equals(attributes.getValue("bold")) ? DevicePrinter.STYLE_BOLD : DevicePrinter.STYLE_PLAIN)
                                    | ("true".equals(attributes.getValue("underline")) ? DevicePrinter.STYLE_UNDERLINE : DevicePrinter.STYLE_PLAIN);
                            String sAlign = attributes.getValue("align");
                            if (null == sAlign) {
                                m_iTextAlign = DevicePrinter.ALIGN_LEFT;
                            } else {
                                m_iTextAlign = switch (sAlign) {
                                    case "right" ->
                                        DevicePrinter.ALIGN_RIGHT;
                                    case "center" ->
                                        DevicePrinter.ALIGN_CENTER;
                                    default ->
                                        DevicePrinter.ALIGN_LEFT;
                                };
                            }
                            m_iTextLength = parseInt(attributes.getValue("length"), 0);
                        }
                        default -> {
                        }
                    }
                }
            }

            case OUTPUT_DISPLAY -> {
                if (null != qName) {
                    switch (qName) {
                        case "line" -> // line 1 or 2 of the display
                            m_sVisorLine = new StringBuffer();
                        case "line1" -> // linea 1 del visor
                            m_sVisorLine = new StringBuffer();
                        case "line2" -> // linea 2 del visor
                            m_sVisorLine = new StringBuffer();
                        case "text" -> {
                            text = new StringBuffer();
                            String sAlign = attributes.getValue("align");
                            if (null == sAlign) {
                                m_iTextAlign = DevicePrinter.ALIGN_LEFT;
                            } else {
                                m_iTextAlign = switch (sAlign) {
                                    case "right" ->
                                        DevicePrinter.ALIGN_RIGHT;
                                    case "center" ->
                                        DevicePrinter.ALIGN_CENTER;
                                    default ->
                                        DevicePrinter.ALIGN_LEFT;
                                };
                            }
                            m_iTextLength = parseInt(attributes.getValue("length"));
                        }
                        default -> {
                        }
                    }
                }
            }

            case OUTPUT_FISCAL -> {
                if (null != qName) {
                    switch (qName) {
                        case "line" -> {
                            text = new StringBuffer();
                            m_dValue1 = parseDouble(attributes.getValue("price"));
                            m_dValue2 = parseDouble(attributes.getValue("units"), 1.0);
                            attribute3 = parseInt(attributes.getValue("tax"));
                        }
                        case "message" ->
                            text = new StringBuffer();
                        case "total" -> {
                            text = new StringBuffer();
                            m_dValue1 = parseDouble(attributes.getValue("paid"));
                        }
                        default -> {
                        }
                    }
                }
            }

        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        switch (m_iOutputType) {
            case OUTPUT_NONE -> {
                if ("play".equals(qName)) {
                    try {
                        @SuppressWarnings("deprecation")
                        AudioClip oAudio = Applet.newAudioClip(getClass().getClassLoader().getResource(text.toString()));
                        oAudio.play();
                    } catch (Exception fnfe) {
                        //throw new ResourceNotFoundException( fnfe.getMessage() );
                    }
                    text = null;
                }
            }
            case OUTPUT_TICKET -> {
                if (null != qName) {
                    switch (qName) {
                        case "image" -> {
                            try {
                                // BufferedImage image = ImageIO.read(getClass().getClassLoader().getResourceAsStream(m_sText.toString()));
                                BufferedImage image = m_system.getResourceAsImage(text.toString());
                                if (image != null) {
                                    m_oOutputPrinter.printImage(image);
                                }
                            } catch (Exception fnfe) {
                                //throw new ResourceNotFoundException( fnfe.getMessage() );
                            }
                            text = null;
                        }
                        case "barcode" -> {
                            m_oOutputPrinter.printBarCode(
                                    bctype,
                                    bcposition,
                                    text.toString());
                            text = null;
                        }
                        case "text" -> {
                            if (m_iTextLength > 0) {
                                switch (m_iTextAlign) {
                                    case DevicePrinter.ALIGN_RIGHT ->
                                        m_oOutputPrinter.printText(m_iTextStyle, DeviceTicket.alignRight(text.toString(), m_iTextLength));
                                    case DevicePrinter.ALIGN_CENTER ->
                                        m_oOutputPrinter.printText(m_iTextStyle, DeviceTicket.alignCenter(text.toString(), m_iTextLength));
                                    default -> // DevicePrinter.ALIGN_LEFT
                                        m_oOutputPrinter.printText(m_iTextStyle, DeviceTicket.alignLeft(text.toString(), m_iTextLength));
                                }
                            } else {
                                m_oOutputPrinter.printText(m_iTextStyle, text.toString());
                            }
                            text = null;
                        }
                        case "line" ->
                            m_oOutputPrinter.endLine();
                        case "ticket" -> {
                            m_oOutputPrinter.endReceipt();
                            m_iOutputType = OUTPUT_NONE;
                            m_oOutputPrinter = null;
                        }
                        default -> {
                        }
                    }
                }
            }
            case OUTPUT_DISPLAY -> {
                if (null != qName) {
                    switch (qName) {
                        case "line" -> {
                            // line 1 or 2 of the display
                            if (m_sVisorLine1 == null) {
                                m_sVisorLine1 = m_sVisorLine.toString();
                            } else {
                                m_sVisorLine2 = m_sVisorLine.toString();
                            }
                            m_sVisorLine = null;
                        }
                        case "line1" -> {
                            // linea 1 del visor
                            m_sVisorLine1 = m_sVisorLine.toString();
                            m_sVisorLine = null;
                        }
                        case "line2" -> {
                            // linea 2 del visor
                            m_sVisorLine2 = m_sVisorLine.toString();
                            m_sVisorLine = null;
                        }
                        case "text" -> {
                            if (m_iTextLength > 0) {
                                switch (m_iTextAlign) {
                                    case DevicePrinter.ALIGN_RIGHT ->
                                        m_sVisorLine.append(DeviceTicket.alignRight(text.toString(), m_iTextLength));
                                    case DevicePrinter.ALIGN_CENTER ->
                                        m_sVisorLine.append(DeviceTicket.alignCenter(text.toString(), m_iTextLength));
                                    default -> // DevicePrinter.ALIGN_LEFT
                                        m_sVisorLine.append(DeviceTicket.alignLeft(text.toString(), m_iTextLength));
                                }
                            } else {
                                m_sVisorLine.append(text);
                            }
                            text = null;
                        }
                        case "display" -> {
                            m_printer.getDeviceDisplay().writeVisor(m_iVisorAnimation, m_sVisorLine1, m_sVisorLine2);
                            m_iVisorAnimation = DeviceDisplayBase.ANIMATION_NULL;
                            m_sVisorLine1 = null;
                            m_sVisorLine2 = null;
                            m_iOutputType = OUTPUT_NONE;
                            m_oOutputPrinter = null;
                        }
                        default -> {
                        }
                    }
                }
            }
            case OUTPUT_FISCAL -> {
                if (null != qName) {
                    switch (qName) {
                        case "fiscalreceipt" -> {
                            m_printer.getFiscalPrinter().endReceipt();
                            m_iOutputType = OUTPUT_NONE;
                        }
                        case "line" -> {
                            m_printer.getFiscalPrinter().printLine(text.toString(), m_dValue1, m_dValue2, attribute3);
                            text = null;
                        }
                        case "message" -> {
                            m_printer.getFiscalPrinter().printMessage(text.toString());
                            text = null;
                        }
                        case "total" -> {
                            m_printer.getFiscalPrinter().printTotal(text.toString(), m_dValue1);
                            text = null;
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (text != null) {
            text.append(ch, start, length);
        }
    }

    private int parseInt(String sValue, int iDefault) {
        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException eNF) {
            return iDefault;
        }
    }

    private int parseInt(String sValue) {
        return parseInt(sValue, 0);
    }

    private double parseDouble(String sValue, double ddefault) {
        try {
            return Double.parseDouble(sValue);
        } catch (NumberFormatException eNF) {
            return ddefault;
        }
    }

    private double parseDouble(String sValue) {
        return parseDouble(sValue, 0.0);
    }

    private String readString(String sValue, String sDefault) {
        if (sValue == null || sValue.equals("")) {
            return sDefault;
        } else {
            return sValue;
        }
    }
}
