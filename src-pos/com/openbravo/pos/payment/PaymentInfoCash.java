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
package com.openbravo.pos.payment;

import com.openbravo.format.Formats;

public class PaymentInfoCash extends PaymentInfo {

    private final double m_dPaid;
    private final double m_dTotal;

    /**
     * Creates a new instance of PaymentInfoCash
     *
     * @param dTotal
     * @param dPaid
     */
    public PaymentInfoCash(double dTotal, double dPaid) {
        m_dTotal = dTotal;
        m_dPaid = dPaid;
    }

    @Override
    public PaymentInfo copyPayment() {
        return new PaymentInfoCash(m_dTotal, m_dPaid);
    }

    @Override
    public String getName() {
        return "cash";
    }

    @Override
    public double getTotal() {
        return m_dTotal;
    }

    public double getPaid() {
        return m_dPaid;
    }

    @Override
    public String getTransactionID() {
        return "no ID";
    }

    public String printPaid() {
        return Formats.CURRENCY.formatValue(m_dPaid);
    }

    public String printChange() {
        return Formats.CURRENCY.formatValue(m_dPaid - m_dTotal);
    }
}
