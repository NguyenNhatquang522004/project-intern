export type LeaveRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type LeaveSession = 'MORNING' | 'AFTERNOON' | 'ALLDAY' | 'ALL_DAY';
export type LeaveType = 'ANNUAL' | 'SICK' | 'UNPAID';

export interface TaskVariables {
  Email?: string;
  EmployeeID?: string;
  FullName?: string;
  Department?: 'IT' | 'HR';
  LeaveType?: LeaveType | LeaveType[] | string | string[];
  StartDate?: string;
  EndDate?: string;
  leaveSession?: LeaveSession;
  totaldays?: number;
  reason?: string;
  Status?: LeaveRequestStatus;
  BusinessKey?: string;
}

export interface UserTask {
  id: string; // Task Key
  name: string; // Tên Task
  processId: string;
  processInstanceKey: string;
  processDefinitionKey: string;
  creationTime: string; // ISO 8601
  assignee: string | null;
  taskState: 'CREATED' | 'COMPLETED' | 'CANCELED';
  formId: string;
  variables: TaskVariables;
}

export interface BaseResponse<T> {
  code: string;
  message: string;
  data: T;
}

export interface FilterSummary {
  allOpen: number;
  assignedToMe: number;
  unassigned: number;
  completed: number;
}
