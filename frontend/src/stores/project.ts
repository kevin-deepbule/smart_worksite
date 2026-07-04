import { defineStore } from 'pinia';
import * as projectApi from '../api/project';
import type { ID, ProjectItem } from '../api/types';

const projectKey = 'smart_worksite_project';

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: [] as ProjectItem[],
    currentProjectId: localStorage.getItem(projectKey) || '',
    loading: false,
    error: ''
  }),
  getters: {
    currentProject: (state) => state.projects.find((item) => String(item.projectId) === String(state.currentProjectId)) || state.projects[0] || null
  },
  actions: {
    async fetchProjects() {
      this.loading = true;
      this.error = '';
      try {
        const page = await projectApi.fetchProjects({ pageNo: 1, pageSize: 100 });
        this.projects = page.records;
        if (!this.currentProject || !this.projects.some((item) => String(item.projectId) === String(this.currentProjectId))) {
          this.switchProject(this.projects[0]?.projectId || '');
        }
        return this.projects;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取项目列表失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    switchProject(projectId: ID) {
      this.currentProjectId = String(projectId);
      if (projectId) localStorage.setItem(projectKey, String(projectId));
      else localStorage.removeItem(projectKey);
    }
  }
});
