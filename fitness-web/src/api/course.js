import request from '@/utils/request'

export function getCourseList(params) {
  return request({ url: '/course/list', method: 'get', params })
}

export function getCourseDetail(id) {
  return request({ url: `/course/${id}`, method: 'get' })
}

export function createCourse(data) {
  return request({ url: '/course/create', method: 'post', data })
}

export function updateCourse(id, data) {
  return request({ url: `/course/${id}`, method: 'put', data })
}

export function deleteCourse(id) {
  return request({ url: `/course/${id}`, method: 'delete' })
}

export function setCourseRecurrence(id, recurrence) {
  return request({ url: `/course/${id}/recurrence`, method: 'put', params: { recurrence } })
}

export function toggleCoursePublished(id) {
  return request({ url: `/course/${id}/publish`, method: 'put' })
}

export function getCourseDates(id) {
  return request({ url: `/course/${id}/dates`, method: 'get' })
}
