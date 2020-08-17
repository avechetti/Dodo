//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
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
package com.openbravo.pos.ticket;

import com.openbravo.pos.taxes.TaxInfo;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.LocalRes;
import com.openbravo.data.loader.SerializableRead;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.payment.PaymentInfo;
import com.openbravo.pos.payment.PaymentInfoMagcard;
import com.openbravo.pos.util.StringUtils;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author adrianromero
 */
public class TicketInfo implements SerializableRead, Externalizable {

    private static final long serialVersionUID = 2765650092387265178L;

    private static final DateFormat m_dateformat = new SimpleDateFormat("hh:mm");
    private String m_sId;
    private TicketTypeInfo tickettype;

    private int m_Number;
    private int m_PointSale;

    private Date m_dDate;
    private Properties attributes;
    private UserInfo m_User;
    private CustomerInfoExt m_Customer;
    private String m_sActiveCash;
    private List<TicketLineInfo> m_aLines;
    private List<PaymentInfo> payments;
    private List<TicketTaxInfo> taxes;
    private UserInfo m_Seller;

    /**
     * Creates new TicketModel
     */
    public TicketInfo() {
        m_sId = UUID.randomUUID().toString();
        tickettype = null;
        m_Number = 0; // incrementamos
        m_dDate = new Date();
        attributes = new Properties();
        m_User = null;
        m_Customer = null;
        m_sActiveCash = null;
        m_aLines = new ArrayList<>(); // vacio de lineas
        payments = new ArrayList<>();
        taxes = null;
        m_Seller = null;

    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        // esto es solo para serializar tickets que no estan en la bolsa de tickets pendientes
        out.writeObject(m_sId);
        out.writeObject(tickettype);
        out.writeInt(m_Number);
        out.writeObject(m_Customer);
        out.writeObject(m_dDate);
        out.writeObject(attributes);
        out.writeObject(m_aLines);
        out.writeObject(m_Seller);
        out.writeInt(m_PointSale);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // esto es solo para serializar tickets que no estan en la bolsa de tickets pendientes
        m_sId = (String) in.readObject();
        tickettype = (TicketTypeInfo) in.readObject();
        m_Number = in.readInt();
        m_Customer = (CustomerInfoExt) in.readObject();
        m_dDate = (Date) in.readObject();
        attributes = (Properties) in.readObject();
        m_aLines = (List<TicketLineInfo>) in.readObject();
        m_User = null;
        m_Seller = (UserInfo) in.readObject();
        m_PointSale = in.readInt();

        m_sActiveCash = null;

        payments = new ArrayList<>();
        taxes = null;

    }

    @Override
    public void readValues(DataRead dr) throws BasicException {
        m_sId = dr.getString(1);
        tickettype = new TicketTypeInfo(dr.getInt(2));
        m_Number = dr.getInt(3);
        m_dDate = dr.getTimestamp(4);
        m_sActiveCash = dr.getString(5);
        try {
            byte[] img = dr.getBytes(6);
            if (img != null) {
                attributes.loadFromXML(new ByteArrayInputStream(img));
            }
        } catch (IOException e) {
        }
        m_User = new UserInfo(dr.getString(7), dr.getString(8));
        m_Customer = new CustomerInfoExt(dr.getString(9));
        m_Seller = new UserInfo(dr.getString(10), dr.getString(11));
        m_aLines = new ArrayList<>();

        payments = new ArrayList<>();
        taxes = null;
    }

    public TicketInfo copyTicket() {
        TicketInfo t = new TicketInfo();

        t.tickettype = tickettype;
        t.m_Number = m_Number;
        t.m_dDate = m_dDate;
        t.m_sActiveCash = m_sActiveCash;
        t.attributes = (Properties) attributes.clone();
        t.m_User = m_User;
        t.m_Customer = m_Customer;

        t.m_aLines = new ArrayList<>();
        m_aLines.forEach((l) -> {
            t.m_aLines.add(l.copyTicketLine());
        });
        t.refreshLines();

        t.payments = new LinkedList<>();
        payments.forEach((p) -> {
            t.payments.add(p.copyPayment());
        });
        t.m_Seller = m_Seller;

        // taxes are not copied, must be calculated again.
        return t;
    }

    public String getId() {
        return m_sId;
    }

    public TicketTypeInfo getTicketType() {
        return tickettype;
    }

    public void setTicketType(TicketTypeInfo tickettype) {
        this.tickettype = tickettype;
    }

    public int getPointSale() {
        return m_PointSale;
    }

    public void setPointSale(int m_PointSale) {
        this.m_PointSale = m_PointSale;
    }

    public int getNumber() {
        return m_Number;
    }

    public void setNumber(int m_Number) {
        this.m_Number = m_Number;
    }

    public String getName(Object info) {

        StringBuilder name = new StringBuilder();

        if (getCustomerId() != null) {
            name.append(m_Customer.toString());
            name.append(" - ");
        }

        if (info == null) {
            if (m_Number == 0) {
                name.append("(").append(m_dateformat.format(m_dDate)).append(" ").append(Long.toString(m_dDate.getTime() % 1000)).append(")");
            } else {
                name.append(Integer.toString(m_Number));
            }
        } else {
            name.append(info.toString());
        }

        return name.toString();
    }

    public String getName() {
        return getName(null);
    }

    public java.util.Date getDate() {
        return m_dDate;
    }

    public void setDate(java.util.Date dDate) {
        m_dDate = dDate;
    }

    public UserInfo getUser() {
        return m_User;
    }

    public void setUser(UserInfo value) {
        m_User = value;
    }

    public UserInfo getSeller() {
        return m_Seller;
    }

    public void setSeller(UserInfo m_Seller) {
        this.m_Seller = m_Seller;
    }

    public CustomerInfoExt getCustomer() {
        return m_Customer;
    }

    public void setCustomer(CustomerInfoExt value) {
        m_Customer = value;
    }

    public String getCustomerId() {
        if (m_Customer == null) {
            return null;
        } else {
            return m_Customer.getId();
        }
    }

    public String getTransactionID() {
        return (getPayments().size() > 0)
                ? (getPayments().get(getPayments().size() - 1)).getTransactionID()
                : StringUtils.getCardNumber(); //random transaction ID
    }

    public String getReturnMessage() {
        return ((getPayments().get(getPayments().size() - 1)) instanceof PaymentInfoMagcard)
                ? ((PaymentInfoMagcard) (getPayments().get(getPayments().size() - 1))).getReturnMessage()
                : LocalRes.getIntString("button.ok");
    }

    public void setActiveCash(String value) {
        m_sActiveCash = value;
    }

    public String getActiveCash() {
        return m_sActiveCash;
    }

    public String getProperty(String key) {
        return attributes.getProperty(key);
    }

    public String getProperty(String key, String defaultvalue) {
        return attributes.getProperty(key, defaultvalue);
    }

    public void setProperty(String key, String value) {
        attributes.setProperty(key, value);
    }

    public Properties getProperties() {
        return attributes;
    }

    public TicketLineInfo getLine(int index) {
        return m_aLines.get(index);
    }

    public void addLine(TicketLineInfo oLine) {

        oLine.setTicket(m_sId, m_aLines.size());
        m_aLines.add(oLine);
    }

    public void insertLine(int index, TicketLineInfo oLine) {
        m_aLines.add(index, oLine);
        refreshLines();
    }

    public void setLine(int index, TicketLineInfo oLine) {
        oLine.setTicket(m_sId, index);
        m_aLines.set(index, oLine);
    }

    public void removeLine(int index) {
        m_aLines.remove(index);
        refreshLines();
    }

    private void refreshLines() {
        for (int i = 0; i < m_aLines.size(); i++) {
            getLine(i).setTicket(m_sId, i);
        }
    }

    public int getLinesCount() {
        return m_aLines.size();
    }

    public double getArticlesCount() {
        double dArticles = 0.0;
        TicketLineInfo oLine;

        for (Iterator<TicketLineInfo> i = m_aLines.iterator(); i.hasNext();) {
            oLine = i.next();
            dArticles += oLine.getMultiply();
        }

        return dArticles;
    }

    public double getSubTotal() {
        double sum = 0.0;
        sum = m_aLines.stream().map((line) -> line.getSubValue()).reduce(sum, (accumulator, _item) -> accumulator + _item);
        return sum;
    }

    public double getTax() {

        double sum = 0.0;
        if (hasTaxesCalculated()) {
            sum = taxes.stream().map((tax) -> tax.getTax()).reduce(sum, (accumulator, _item) -> accumulator + _item); // Taxes are already rounded...
        } else {
            sum = m_aLines.stream().map((line) -> line.getTax()).reduce(sum, (accumulator, _item) -> accumulator + _item);
        }
        return sum;
    }

    public double getTotal() {

        return getSubTotal() + getTax();
    }

    public double getTotalPaid() {

        double sum = 0.0;
        sum = payments.stream().filter((p) -> (!"debtpaid".equals(p.getName()))).map((p) -> p.getTotal()).reduce(sum, (accumulator, _item) -> accumulator + _item);
        return sum;
    }

    public List<TicketLineInfo> getLines() {
        return m_aLines;
    }

    public void setLines(List<TicketLineInfo> l) {
        m_aLines = l;
    }

    public List<PaymentInfo> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentInfo> l) {
        payments = l;
    }

    public void resetPayments() {
        payments = new ArrayList<>();
    }

    public List<TicketTaxInfo> getTaxes() {
        return taxes;
    }

    public boolean hasTaxesCalculated() {
        return taxes != null;
    }

    public void setTaxes(List<TicketTaxInfo> l) {
        taxes = l;
    }

    public void resetTaxes() {
        taxes = null;
    }

    public TicketTaxInfo getTaxLine(TaxInfo tax) {

        for (TicketTaxInfo taxline : taxes) {
            if (tax.getId().equals(taxline.getTaxInfo().getId())) {
                return taxline;
            }
        }

        return new TicketTaxInfo(tax);
    }

    public TicketTaxInfo[] getTaxLines() {

        Map<String, TicketTaxInfo> m = new HashMap<>();

        TicketLineInfo oLine;
        for (Iterator<TicketLineInfo> i = m_aLines.iterator(); i.hasNext();) {
            oLine = i.next();

            TicketTaxInfo t = m.get(oLine.getTaxInfo().getId());
            if (t == null) {
                t = new TicketTaxInfo(oLine.getTaxInfo());
                m.put(t.getTaxInfo().getId(), t);
            }
            t.add(oLine.getSubValue());
        }

        // return dSuma;
        Collection<TicketTaxInfo> avalues = m.values();
        return avalues.toArray(new TicketTaxInfo[avalues.size()]);
    }

    public String printNumber() {
        if (m_Number > 0) {
            // valid ticket id
            return Formats.INT.formatValue(m_Number);
        } else {
            return "";
        }
    }

    public String printDate() {
        return Formats.TIMESTAMP.formatValue(m_dDate);
    }

    public String printUser() {
        return m_User == null ? "" : m_User.getName();
    }

    public String printCustomer() {
        return m_Customer == null ? "" : m_Customer.getName();
    }

    public String printArticlesCount() {
        return Formats.DOUBLE.formatValue(getArticlesCount());
    }

    public String printSubTotal() {
        return Formats.CURRENCY.formatValue(getSubTotal());
    }

    public String printTax() {
        return Formats.CURRENCY.formatValue(getTax());
    }

    public String printTotal() {
        return Formats.CURRENCY.formatValue(getTotal());
    }

    public String printTotalPaid() {
        return Formats.CURRENCY.formatValue(getTotalPaid());
    }

}
