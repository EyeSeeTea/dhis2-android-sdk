package org.hisp.dhis.client.sdk.models.pager;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pager {

    @JsonProperty("page")
    private String page;

    @JsonProperty("isLastPage")
    private String isLastPage;

    @JsonProperty("pageCount")
    private String pageCount;

    @JsonProperty("total")
    private String total;

    @JsonProperty("pageSize")
    private String pageSize;

    public Pager() {
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPageCount() {
        return pageCount;
    }

    public void setPageCount(String pageCount) {
        this.pageCount = pageCount;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getIsLastPage() {
        return isLastPage;
    }

    public void setIsLastPage(String isLastPage) {
        this.isLastPage = isLastPage;
    }
}
