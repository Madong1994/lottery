package com.md.luck.lottery.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.md.luck.lottery.common.Cont;
import com.md.luck.lottery.common.ResponMsg;
import com.md.luck.lottery.common.entity.ActivityAddRecord;
import com.md.luck.lottery.common.entity.CastCulp;
import com.md.luck.lottery.common.entity.Customer;
import com.md.luck.lottery.common.entity.request.HelpGrab;
import com.md.luck.lottery.common.entity.respons.CastCulpChild;
import com.md.luck.lottery.common.util.MaObjUtil;
import com.md.luck.lottery.mapper.ActivMapper;
import com.md.luck.lottery.mapper.ActivityAddRecordMapper;
import com.md.luck.lottery.mapper.CastCulpMapper;
import com.md.luck.lottery.mapper.CustomerMapper;
import com.md.luck.lottery.service.CastCulpService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSessionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CastCulpServiceImpl implements CastCulpService {
    private Log log = LogFactory.getLog(this.getClass());
    @Autowired
    CustomerMapper customerMapper;
    @Autowired
    ActivMapper activMapper;
    @Autowired
    CastCulpMapper castCulpMapper;
    @Autowired
    ActivityAddRecordMapper activityAddRecordMapper;

    @Transactional(rollbackFor = SqlSessionException.class)
    @Override
    public ResponMsg culp(String teamPlayerOpenid, HelpGrab helpGrab) {
        if (MaObjUtil.hasEmpty(teamPlayerOpenid, helpGrab) && MaObjUtil.hasEmpty(helpGrab.getActivId(), helpGrab.getOepnid())) {
            return ResponMsg.newFail(null).setMsg("缺省参数");
        }
        if (teamPlayerOpenid.equals(helpGrab.getOepnid())) {
            return ResponMsg.newFail(null).setMsg("已经参与过此活动");
        }
        ResponMsg responMsg = null;
        try {
            Customer customer = customerMapper.queryByOpenid(teamPlayerOpenid);
            if (MaObjUtil.isEmpty(customer)) {
                return ResponMsg.newFail(null).setMsg("用户信息异常");
            }
            // 助力数量
            int ran = RandomUtil.randomEle(Cont.RANDOM_LIMIT);
            // 修改活动人气数
            int popularity = activMapper.queryPopularity(helpGrab.getActivId());
            popularity ++;
            activMapper.updatePopularity(popularity, helpGrab.getActivId());
            // 更新参与记录中助力数量，助力人数
            ActivityAddRecord activityAddRecordYs = activityAddRecordMapper.queryTeamMateCountAndCulp(helpGrab.getGrabRecordId(), helpGrab.getOepnid());
            int teamMateCount = activityAddRecordYs.getTeamMateCount() + 1;
            int culpNum = activityAddRecordYs.getCulp() + ran;
            activityAddRecordMapper.updateCulpAndTeamMateCount(culpNum, teamMateCount, helpGrab.getGrabRecordId(), helpGrab.getOepnid());
            //更新排名
            ActivityAddRecord activityAddRecord = activityAddRecordMapper.queryTeamMateCountAndRank(helpGrab.getGrabRecordId(), helpGrab.getOepnid());
            activityAddRecordMapper.updateCulpRank(activityAddRecord.getRank(), helpGrab.getGrabRecordId(), helpGrab.getOepnid());
            // 新增助力记录
            CastCulpChild castCulpChild = new CastCulpChild();
            castCulpChild.setActAddRecordId(helpGrab.getGrabRecordId());
            castCulpChild.setActivid(helpGrab.getActivId());
            castCulpChild.setCastCulp(ran);
            castCulpChild.setNickName(customer.getNickName());
            castCulpChild.setIcon(customer.getIcon());
            castCulpChild.setOpenid(teamPlayerOpenid);
            castCulpMapper.insert(castCulpChild);
            castCulpChild.setTotalCulp(activityAddRecord.getCulp());
            castCulpChild.setRank(activityAddRecord.getRank());
            responMsg = ResponMsg.newSuccess(castCulpChild);
        }catch (SqlSessionException e) {
            responMsg = ResponMsg.newFail(null).setMsg("操作失败");
            log.error(e.getMessage());
        }
        return responMsg;
    }

    @Override
    public ResponMsg queryPage( Integer pageNum, Integer pageSize, Long recordId) {
        if (MaObjUtil.hasEmpty(pageNum, pageSize, recordId)) {
            return ResponMsg.newFail(null).setMsg("缺省参数");
        }
        pageSize = pageSize > Cont.MAX_PAGE_SIZE ? Cont.MAX_PAGE_SIZE: pageSize;
        ResponMsg responMsg = null;
        try {
            PageHelper.startPage(pageNum, pageSize);
            List<CastCulp> castCulpList = castCulpMapper.queryByRecordId(recordId);
            PageInfo<CastCulp> pageInfo = new PageInfo<CastCulp>(castCulpList);

            responMsg = ResponMsg.newSuccess(pageInfo);
        }catch (SqlSessionException e) {
            responMsg = ResponMsg.newFail(null).setMsg("操作失败");
            log.error(e.getMessage());
        }
        return responMsg;
    }

    public static void main(String[] args) {
        for (int i = 0; i<1000; i++) {

            System.out.println(RandomUtil.randomEle(Cont.RANDOM_LIMIT));
        }
    }
}
