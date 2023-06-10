package com.fluidbpm.fluidwebkit.backing.bean;

import com.fluidbpm.program.api.vo.ABaseFluidVO;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class ABaseLDM<T extends ABaseFluidVO> extends LazyDataModel<T> {

	@Getter
	protected List<T> dataListing;

	@Setter
	private Comparator comparator;

	public ABaseLDM() {
		this(new ArrayList<>());
	}

	public ABaseLDM(List<T> initialListing) {
		this.dataListing = initialListing;
	}

	@Override
	public T getRowData(String rowKey) {
		if (this.dataListing == null) return null;

		Long idAsLong = Long.parseLong(rowKey.trim());
		return this.dataListing.stream()
				.filter(itm -> idAsLong.equals(itm.getId()))
				.findFirst()
				.orElse(null);
	}

	@Override
	public String getRowKey(T object) {
		if (object.getId() == null) return null;

		return object.getId().toString();
	}

	public void addToInitialListing(List<T> listingToAdd) {
		if (listingToAdd == null || listingToAdd.isEmpty()) return;

		this.dataListing.addAll(listingToAdd);
	}

	public void addToInitialListing(T toAdd) {
		if (toAdd == null) return;

		this.dataListing.add(toAdd);
	}

	public void clearInitialListing() {
		if (dataListing == null) return;

		this.dataListing.clear();
	}

	@Override
	public List<T> load(
		int first,
		int pageSize,
		Map<String, SortMeta> sortMeta,
		Map<String, FilterMeta> filters
	) {
		this.setRowCount(0);
		if (this.dataListing == null) return null;

		if (this.dataListing.isEmpty()) return this.dataListing;

		if (this.comparator != null) this.dataListing.sort(this.comparator);

		int totalSize = this.dataListing.size();
		this.setRowCount(totalSize);
		int toVal = (first + pageSize);
		List<T> returnVal = this.dataListing.subList(first, toVal > totalSize ? totalSize : toVal);
		return returnVal;
	}

	@Override
	public int count(Map<String, FilterMeta> map) {
		return this.getRowCount();
	}
}
