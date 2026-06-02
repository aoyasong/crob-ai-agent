import request from '@/config/axios'

export interface CreateTaskReq {
  scenario: string
  inputs: Record<string, any>
}

export interface TaskVO {
  taskId: number
  scenario: string
  status: string
  inputsJson: Record<string, any>
  currentAttemptId: number | null
  latestSuccessAttemptId: number | null
  disposition: string
  missingFields: string[]
  errorCode: string
  errorMessage: string
  reportUrl: string
}

export interface AttemptVO {
  attemptId: number
  taskId: number
  scenario: string
  status: string
  startedAt: string
  endedAt: string
  errorCode: string
  errorMessage: string
}

// 创建任务
export const createTask = (data: CreateTaskReq, idempotencyKey?: string) => {
  return request.post({
    url: '/tasks',
    data,
    headers: idempotencyKey
      ? { 'Idempotency-Key': idempotencyKey }
      : {}
  })
}

// 补参
export const submitInputs = (taskId: number, inputs: Record<string, any>) => {
  return request.post({ url: `/tasks/${taskId}/inputs`, data: { inputs } })
}

// 查询任务
export const getTask = (taskId: number) => {
  return request.get({ url: `/tasks/${taskId}` })
}

// 查询 attempts
export const listAttempts = (taskId: number) => {
  return request.get({ url: `/tasks/${taskId}/attempts` })
}

// 取消任务
export const cancelTask = (taskId: number, attemptId: number) => {
  return request.post({ url: `/tasks/${taskId}/cancel?attempt_id=${attemptId}` })
}

// 重跑任务
export const rerunTask = (taskId: number, constraints?: Record<string, any>) => {
  return request.post({ url: `/tasks/${taskId}/rerun`, data: constraints || {} })
}

// SSE 流式订阅地址
export const getStreamUrl = (taskId: number, attemptId: number, baseUrl: string, apiPrefix: string) => {
  return `${baseUrl}${apiPrefix}/tasks/${taskId}/stream?attempt_id=${attemptId}`
}

// 报告地址
export const getReportUrl = (taskId: number, attemptId: number, baseUrl: string) => {
  return `${baseUrl}/tasks/${taskId}/report?attempt_id=${attemptId}`
}
