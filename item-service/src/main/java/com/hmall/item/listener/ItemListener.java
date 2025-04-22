package com.hmall.item.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.constants.ElasticConstants;
import com.hmall.common.constants.MQConstants;
import com.hmall.item.domain.dto.ItemMQDto;
import com.hmall.item.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ItemListener {

    private final RestHighLevelClient client;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_ORDER_QUEUE_NAME,durable = "true"),
            exchange = @Exchange(name = MQConstants.ITEM_EXCHANGE_NAME,delayed = "false"),
            key = MQConstants.ITEM_QUEUE_KEY
    ))
    public void listenerItemMessage(ItemMQDto itemMQDto){
        if(itemMQDto.getItemDTO().getId() == null) return;
        switch(itemMQDto.getOperate()){
            case ADD:
                addItemByIndex(itemMQDto.getItemDTO());
                break;
            case REMOVE:
                removeItemByIndex(itemMQDto.getItemDTO());
                break;
            case UPDATE:
                updateItemByIndex(itemMQDto.getItemDTO());
                break;
            default:
                log.error("未知的操作类型");
        }
    }

    private void updateItemByIndex(ItemDTO itemDTO) {
        UpdateRequest request = new UpdateRequest(ElasticConstants.ITEM_INDEX_NAME,itemDTO.getId().toString());
        ItemDoc itemDoc = BeanUtil.copyProperties(itemDTO, ItemDoc.class);
        itemDoc.setUpdateTime(LocalDateTime.now());
        request.doc(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        try {
            client.update(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("修改索引库中商品出错了:{}",e.getMessage());
        }
    }

    private void addItemByIndex(ItemDTO itemDTO) {
        IndexRequest request = new IndexRequest(ElasticConstants.ITEM_INDEX_NAME);
        ItemDoc itemDoc = BeanUtil.copyProperties(itemDTO, ItemDoc.class);
        request.id(itemDoc.getId());
        request.source(JSONUtil.toJsonStr(itemDoc),XContentType.JSON);
        try {
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("添加商品到索引库出错了:{}",e.getMessage());
        }
    }

    private void removeItemByIndex(ItemDTO itemDTO) {
        DeleteRequest request = new DeleteRequest(ElasticConstants.ITEM_INDEX_NAME);
        request.id(itemDTO.getId().toString());
        try {
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
