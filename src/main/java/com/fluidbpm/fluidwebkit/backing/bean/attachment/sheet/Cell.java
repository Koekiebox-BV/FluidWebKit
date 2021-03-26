package com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cell implements Serializable {
	private String value;
	private String headerText;

	public Cell(String value) {
		this.value = value;
	}
}
