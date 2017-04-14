package com.gentics.mesh.core.data.page;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * A page is the result of a query which returns paged data. Each page may contain multiple items. A page can be transformed into a rest response.
 */
public interface Page<T> extends Iterable<T> {

	/**
	 * Return the per page parameter value.
	 * 
	 * @return
	 */
	int getPerPage();

	/**
	 * Return the number of element which this page is currently providing.
	 * 
	 * @return
	 */
	long getNumberOfElements();

	/**
	 * Return the total amount of pages which the resources that provided this page could return.
	 * 
	 * @return
	 */
	long getTotalPages();

	/**
	 * Return the current page number.
	 * 
	 * @return
	 */
	long getNumber();

	/**
	 * Return the total item count which the resource that provided this page could return.
	 * 
	 * @return
	 */
	long getTotalElements();

	/**
	 * Return the page size.
	 * 
	 * @return
	 */
	int getSize();

	/**
	 * Set the paging parameters into the given list response by examining the given page.
	 * 
	 * @param response
	 *            List response that will be updated
	 */
	void setPaging(ListResponse<?> response);

	/**
	 * Apply paging to the list of elements.
	 * 
	 * @param elementList
	 * @param pagingInfo
	 * @return
	 */
	public static <R> Page<? extends R> applyPaging(List<R> elementList, PagingParameters pagingInfo) {
		// Internally we start with page 0
		int page = pagingInfo.getPage() - 1;

		int low = page * pagingInfo.getPerPage();
		int upper = low + pagingInfo.getPerPage() - 1;

		int n = 0;

		List<R> pagedList = new ArrayList<>();
		for (R element : elementList) {

			// Only add elements that are within the page
			if (n >= low && n <= upper) {
				pagedList.add(element);
			}
			n++;
		}

		// Set meta information to the rest response
		int totalPages = (int) Math.ceil(elementList.size() / (double) pagingInfo.getPerPage());
		// Cap totalpages to 1
		totalPages = totalPages == 0 ? 1 : totalPages;

		return new PageImpl<>(pagedList, n, pagingInfo.getPage(), totalPages, elementList.size(), pagingInfo.getPerPage());
	}

	/**
	 * Returns the wrapped list which contains the results.
	 * @return
	 */
	List<? extends T> getWrappedList();

}
