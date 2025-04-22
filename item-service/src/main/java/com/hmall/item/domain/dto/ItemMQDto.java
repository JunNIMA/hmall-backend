package com.hmall.item.domain.dto;

import com.hmall.api.dto.ItemDTO;
import com.hmall.item.domain.ItemOperate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemMQDto{

    private ItemOperate operate;

    private ItemDTO itemDTO;

}
