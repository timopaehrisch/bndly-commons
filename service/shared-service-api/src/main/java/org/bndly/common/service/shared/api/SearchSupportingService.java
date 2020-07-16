package org.bndly.common.service.shared.api;

/*-
 * #%L
 * Service Shared API
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.ArrayList;
import java.util.List;

public interface SearchSupportingService<MODEL> {

	public static class Pagination {

		private Long pageSize;
		private Long currentPageIndex;
		private Long totalPages;

		public Long getPageSize() {
			return pageSize;
		}

		public void setPageSize(Long pageSize) {
			this.pageSize = pageSize;
		}

		public Long getCurrentPageIndex() {
			return currentPageIndex;
		}

		public void setCurrentPageIndex(Long currentPageIndex) {
			this.currentPageIndex = currentPageIndex;
		}

		public Long getTotalPages() {
			return totalPages;
		}

		public void setTotalPages(Long totalPages) {
			this.totalPages = totalPages;
		}
	}

	public static class SearchResult<D> {

		private Pagination pagination;
		private String query;
		private long totalHits;
		private List<D> items = new ArrayList<>();

		public long getTotalHits() {
			return totalHits;
		}

		public void setTotalHits(long totalHits) {
			this.totalHits = totalHits;
		}

		public List<D> getItems() {
			return items;
		}

		public void setItems(List<D> items) {
			this.items = items;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public String getQuery() {
			return query;
		}

		public Pagination getPagination() {
			return pagination;
		}

		public void setPagination(Pagination pagination) {
			this.pagination = pagination;
		}

	}

	public SearchResult<MODEL> search(String q, long start, long hitsPerPage);
}
