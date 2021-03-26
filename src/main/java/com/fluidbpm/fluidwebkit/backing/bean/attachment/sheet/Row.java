package com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Row implements Serializable {
	public List<Cell> cells;
}
