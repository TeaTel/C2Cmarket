package com.campus.backend.mapper;

import com.campus.backend.entity.Favorite;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 商品收藏数据访问层
 */
@Mapper
public interface FavoriteMapper {

    /**
     * 收藏商品
     */
    @Insert("INSERT INTO favorites (user_id, product_id) VALUES (#{userId}, #{productId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Favorite favorite);

    /**
     * 取消收藏
     */
    @Delete("DELETE FROM favorites WHERE user_id = #{userId} AND product_id = #{productId}")
    int delete(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 检查是否已收藏
     */
    @Select("SELECT COUNT(*) > 0 FROM favorites WHERE user_id = #{userId} AND product_id = #{productId}")
    boolean exists(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 查询用户的收藏列表
     */
    @Select("SELECT * FROM favorites WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Favorite> selectByUserId(
        @Param("userId") Long userId,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    /**
     * 统计用户收藏数
     */
    @Select("SELECT COUNT(*) FROM favorites WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计商品的收藏数
     */
    @Select("SELECT COUNT(*) FROM favorites WHERE product_id = #{productId}")
    int countByProductId(@Param("productId") Long productId);
}
