package com.taotao.content.service.impl;

import java.util.Date;
import java.util.List;

import com.alibaba.dubbo.common.json.JSON;
import com.taotao.common.utils.JsonUtils;
import com.taotao.jedis.service.JedisClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.taotao.common.pojo.EasyUIDataGridResult;
import com.taotao.common.pojo.TaotaoResult;
import com.taotao.content.service.ContentService;
import com.taotao.mapper.TbContentMapper;
import com.taotao.pojo.TbContent;
import com.taotao.pojo.TbContentExample;
import com.taotao.pojo.TbContentExample.Criteria;

@Service
public class ContentServiceImpl implements ContentService {
    @Autowired
    private TbContentMapper contentMapper;

    @Autowired
    private JedisClient jedisClient;

    @Value("${INDEX_CONTENT}")
    private String INDEX_CONTENT;

    @Override
    public List<TbContent> getContentListByCid(long cid) {
        //首先查询缓存，如果缓存中存在的话，就直接将结果返回给前台展示，查询缓存不能影响业务流程
        try {
            String json = jedisClient.hget(INDEX_CONTENT, cid+"");
            //如果从缓存中查到了结果
            if(StringUtils.isNotBlank(json)){
                //将json串转化为List<TbContent>
                List<TbContent> list = JsonUtils.jsonToList(json, TbContent.class);
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        TbContentExample example = new TbContentExample();
        Criteria criteria = example.createCriteria();
        criteria.andCategoryIdEqualTo(cid);
        List<TbContent> list = contentMapper.selectByExample(example);
        //添加缓存，不能影响业务流程
        try {
            String json = JsonUtils.objectToJson(list);
            jedisClient.hset(INDEX_CONTENT, cid+"", json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回结果
        return list;
    }

    @Override
    public EasyUIDataGridResult getContentList(long categoryId, int page, int rows) {
        //设置分页信息
        PageHelper.startPage(page, rows);
        //执行查询
        TbContentExample example = new TbContentExample();
        Criteria createCriteria = example.createCriteria();
        createCriteria.andCategoryIdEqualTo(categoryId);
        //获取查询结果
        List<TbContent> list = contentMapper.selectByExample(example);
        PageInfo<TbContent> pageInfo = new PageInfo<>(list);
        EasyUIDataGridResult result = new EasyUIDataGridResult();
        result.setRows(list);
        result.setTotal(pageInfo.getTotal());
        //返回结果
        return result;
    }

    @Override
    public TaotaoResult addContent(TbContent content) {
        //补充属性
        content.setCreated(new Date());
        content.setUpdated(new Date());
        //添加
        contentMapper.insert(content);
        jedisClient.hdel(INDEX_CONTENT, content.getCategoryId().toString());
        //返回结果
        return TaotaoResult.ok();
    }

    @Override
    public TaotaoResult updateContent(TbContent content) {
        // 填充属性
        content.setUpdated(new Date());
        //更新内容
        contentMapper.updateByPrimaryKey(content);
        //返回结果
        return TaotaoResult.ok();
    }

    @Override
    public TaotaoResult deleteContent(String ids) {
        String[] idList = ids.split(",");
        for(String id : idList){
            //删除内容
            contentMapper.deleteByPrimaryKey(Long.valueOf(id));
        }
        //返回结果
        return TaotaoResult.ok();
    }

    @Override
    public TaotaoResult getContent(long id) {
        TbContent content = contentMapper.selectByPrimaryKey(id);
        return TaotaoResult.ok(content);
    }

}