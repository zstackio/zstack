package org.zstack.sdk;

public class Spending  {

    public java.lang.String spendingType;
    public void setSpendingType(java.lang.String spendingType) {
        this.spendingType = spendingType;
    }
    public java.lang.String getSpendingType() {
        return this.spendingType;
    }

    public double spending;
    public void setSpending(double spending) {
        this.spending = spending;
    }
    public double getSpending() {
        return this.spending;
    }

    public java.lang.Long dateStart;
    public void setDateStart(java.lang.Long dateStart) {
        this.dateStart = dateStart;
    }
    public java.lang.Long getDateStart() {
        return this.dateStart;
    }

    public java.lang.Long dateEnd;
    public void setDateEnd(java.lang.Long dateEnd) {
        this.dateEnd = dateEnd;
    }
    public java.lang.Long getDateEnd() {
        return this.dateEnd;
    }

    public java.util.List<SpendingDetails> details;
    public void setDetails(java.util.List<SpendingDetails> details) {
        this.details = details;
    }
    public java.util.List<SpendingDetails> getDetails() {
        return this.details;
    }

}
