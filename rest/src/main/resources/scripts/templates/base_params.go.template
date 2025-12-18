// Copyright (c) ZStack.io, Inc.

package param

import (
	"errors"
	"fmt"
	"net/url"
	"reflect"
	"strings"

	"github.com/fatih/structs"
)

type QueryParam struct {
	url.Values
}

func NewQueryParam() QueryParam {
	return QueryParam{
		Values: make(url.Values),
	}
}

// AddQ adds a query condition, similar to a MySQL database query.
// Omitting this field will return all records, with the number of returned records limited by the 'limit' field.
func (params *QueryParam) AddQ(q string) *QueryParam {
	if params.Get("q") == "" {
		params.Set("q", q)
	} else {
		params.Add("q", q)
	}
	return params
}

// Limit sets the maximum number of records to return, similar to MySQL's 'limit'. Default value is 1000.
func (params *QueryParam) Limit(limit int) *QueryParam {
	params.Set("limit", fmt.Sprintf("%d", limit))
	return params
}

// Start sets the starting position for the query, similar to MySQL's 'offset'. Used with 'limit' for pagination.
func (params *QueryParam) Start(start int) *QueryParam {
	params.Set("start", fmt.Sprintf("%d", start))
	return params
}

// Count sets the query to return the count of records that match the query conditions, similar to MySQL's 'count()' function.
func (params *QueryParam) Count(count bool) *QueryParam {
	params.Set("count", fmt.Sprintf("%t", count))
	return params
}

// GroupBy groups the results by a specified field, similar to MySQL's 'group by' keyword.
func (params *QueryParam) GroupBy(groupBy string) *QueryParam {
	params.Set("groupBy", groupBy)
	return params
}

// ReplyWithCount, when set to true, includes the total count of records that match the query in the response.
func (params *QueryParam) ReplyWithCount(replyWithCount bool) *QueryParam {
	params.Set("replyWithCount", fmt.Sprintf("%t", replyWithCount))
	return params
}

// FilterName sets a filter name, functionality is unknown from ZStack Java SDK (sdk-4.4.0.jar).
func (params *QueryParam) FilterName(filterName string) *QueryParam {
	params.Set("filterName", filterName)
	return params
}

// Sort sorts the results by a specified field, similar to MySQL's 'sort by' keyword.
// Use '+' for ascending order and '-' for descending order, followed by the field name.
func (params *QueryParam) Sort(sort string) *QueryParam {
	params.Set("sort", sort)
	return params
}

// Fields specifies the fields to return, similar to MySQL's 'select' fields functionality.
func (params *QueryParam) Fields(fields []string) *QueryParam {
	params.Set("fields", strings.Join(fields, ","))
	return params
}

// ConvertStruct2UrlValues converts a struct to url.Values.
func ConvertStruct2UrlValues(param interface{}) (url.Values, error) {
	if reflect.Ptr != reflect.TypeOf(param).Kind() {
		return nil, errors.New("model should be pointer kind")
	}
	result := url.Values{}
	if param == nil || reflect.ValueOf(param).IsNil() {
		return nil, errors.New("param is nil")
	}

	s := structs.New(param)
	s.TagName = "json"
	mappedOpts := s.Map()
	for k, v := range mappedOpts {
		result.Set(k, fmt.Sprintf("%v", v))
	}
	return result, nil
}
