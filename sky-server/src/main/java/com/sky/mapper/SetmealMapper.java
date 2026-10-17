package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;              // 【修改】新增：自动填充注解
import com.sky.enumeration.OperationType;      // 【修改】新增：操作类型枚举
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 新增套餐
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)   // 【修改】新增：插入时自动填充 create_time/create_user/update_time/update_user
    void insert(Setmeal setmeal);

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Select(("select  * from setmeal where id = #{id}"))
    Setmeal getById(Long id);

    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatchByIds(List<Long> ids);

    /**
     * 修改套餐
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)   // 【修改】新增：更新时自动填充 update_time/update_user
    void update(Setmeal setmeal);
}
