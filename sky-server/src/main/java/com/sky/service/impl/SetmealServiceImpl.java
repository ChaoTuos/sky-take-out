package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;   // 【修改】新增：事务注解

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;


    /**
     * 新增套餐 同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Override
    @Transactional   // 【修改】新增：新增套餐涉及 setmeal、setmeal_dish 两张表，需事务
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal); // 复制属性

        //将数据插入到套餐表
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        //将套餐和菜品的关联关系插入到套餐菜品关联表
        setmealDTO.getSetmealDishes().forEach(
                setmealDish -> setmealDish.setSetmealId(setmealId)
                );
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);


        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    @Transactional   // 【修改】新增：批量删除涉及两张表，需事务
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            Setmeal setmeal =  setmealMapper.getById(id);
            //起售中的不可删除
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                //抛出异常
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //删除套餐表中的数据
        setmealMapper.deleteBatchByIds(ids);

        //删除套餐菜品关联表中的数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据id查询套餐以及套餐菜品关联信息
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //查询套餐信息
        Setmeal setmeal = setmealMapper.getById(id);
        //查询套餐菜品关联信息
        List<SetmealDish> setmealDishList = setmealDishMapper.getBySetmealId(id);

        SetmealVO setmealVo = new SetmealVO();

        BeanUtils.copyProperties(setmeal, setmealVo);
        setmealVo.setSetmealDishes(setmealDishList);

        return setmealVo;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    @Transactional   // 【修改】新增：修改套餐涉及两张表，需事务
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //更新套餐表中的数据
        setmealMapper.update(setmeal);

        //删除套餐菜品关联表中的数据
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()){
            setmealDishes.forEach(setmealDish ->
                    setmealDish.setSetmealId(setmealDTO.getId()));
            //向套餐菜品关联表插入n条数据
            setmealDishMapper.insertBatch(setmealDishes);
        }



    }

    /**
     * 套餐起售/停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        ////起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && !dishList.isEmpty()){
                //套餐内包含未启售菜品，无法启售
                for (Dish dish : dishList) {
                    if(dish.getStatus() == StatusConstant.DISABLE){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }

        //更新套餐表中的数据
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }
}
