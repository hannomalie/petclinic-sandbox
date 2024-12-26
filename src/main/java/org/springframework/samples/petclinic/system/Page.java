package org.springframework.samples.petclinic.system;

import java.util.Iterator;
import java.util.List;

public class Page<T> {
	private final List<T> values;
	private final int totalPages;

	public Page(List<T> values, Pageable pageable) {
		this.values = values;
		this.totalPages = 1;
	}

	public Page(List<T> values) {
		this.values = values;
		this.totalPages = 1;
	}

	public List<T> toList() {
		return values;
	}

	public List<T> getContent() {
		return values;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public int getTotalElements() {
		return values.size();
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public Iterator<T> iterator() {
		return values.iterator();
	}
}
