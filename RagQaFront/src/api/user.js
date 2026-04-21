import request from '../utils/request'

export const userApi = {
  // 用户登录
  login(data) {
    return request({
      url: '/user/login',
      method: 'post',
      data
    })
  },

  // 用户注册
  register(data) {
    return request({
      url: '/user/register',
      method: 'post',
      data
    })
  },

  // 获取用户信息
  getUserInfo(id) {
    return request({
      url: `/user/${id}`,
      method: 'get'
    })
  },

  // 更新用户信息
  updateUser(id, data) {
    return request({
      url: `/user/${id}`,
      method: 'put',
      data
    })
  },

  // 修改密码
  changePassword(id, data) {
    return request({
      url: `/user/${id}/password`,
      method: 'put',
      data
    })
  },

  // 注销账号
  deleteUser(id) {
    return request({
      url: `/user/${id}`,
      method: 'delete'
    })
  },

  // 获取用户列表（管理员）
  getUserList(params) {
    return request({
      url: '/user/list',
      method: 'get',
      params
    })
  },

    // 获取用户统计数据
  getUserStats(id) {
    return request({
      url: `/user/${id}/stats`,
      method: 'get'
    })
  }
}