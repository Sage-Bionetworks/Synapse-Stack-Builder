package org.sagebionetworks.template;

import java.util.Iterator;
import java.util.List;

/**
 * An Iterator driven by a PageProvider.
 *
 * @param <T>
 */
public class PageIterator<T> implements Iterator<T> {
	
	private final PageProvider<T> lambda;
	private Iterator<T> page;
	private boolean isDone;
	
	public PageIterator(PageProvider<T> lambda) {
		this.lambda = lambda;
		this.isDone = false;
	}

	@Override
	public boolean hasNext() {
		if(isDone) {
			return false;
		}
		if(page == null) {
			List<T> newPage = lambda.nextPage();
			if(newPage.isEmpty()) {
				isDone = true;
				return false;
			}
			page = newPage.iterator();
		}
		if(!page.hasNext()) {
			page = null;
			return hasNext();
		}
		return true;
	}

	@Override
	public T next() {
		return page.next();
	}

	@FunctionalInterface
	public interface PageProvider<T> {

		/**
		 * Provide the next page of results;
		 * @return
		 */
		List<T> nextPage();
	}

}
