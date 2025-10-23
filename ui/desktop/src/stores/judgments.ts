import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { Judgment, CreateJudgmentRequest, JudgmentFilters, JudgmentSortOptions } from '@/types/judgments';
import { ApiService } from '@/services/api';

interface JudgmentsState {
  // Data
  judgments: Judgment[];
  loading: boolean;
  error: string | null;
  
  // Pagination
  pagination: {
    page: number;
    per_page: number;
    total: number;
    total_pages: number;
  };
  
  // Filters and sorting
  filters: JudgmentFilters;
  sortOptions: JudgmentSortOptions;
  
  // Actions
  fetchJudgments: (eventId?: string, page?: number, perPage?: number) => Promise<void>;
  createJudgment: (judgmentData: CreateJudgmentRequest) => Promise<Judgment | null>;
  updateJudgment: (id: string, judgmentData: Partial<CreateJudgmentRequest>) => Promise<void>;
  deleteJudgment: (id: string) => Promise<void>;
  
  // Filter and sort actions
  setFilters: (filters: Partial<JudgmentFilters>) => void;
  setSortOptions: (sortOptions: JudgmentSortOptions) => void;
  clearFilters: () => void;
  
  // Utility actions
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

const defaultFilters: JudgmentFilters = {};
const defaultSortOptions: JudgmentSortOptions = {
  field: 'submitted_at',
  direction: 'desc'
};

export const useJudgmentsStore = create<JudgmentsState>()(
  devtools(
    (set) => ({
      // Initial state
      judgments: [],
      loading: false,
      error: null,
      pagination: {
        page: 1,
        per_page: 20,
        total: 0,
        total_pages: 0
      },
      filters: defaultFilters,
      sortOptions: defaultSortOptions,

      // Actions
      fetchJudgments: async (eventId?: string, page = 1, perPage = 20) => {
        set({ loading: true, error: null });
        
        try {
          const response = await ApiService.getJudgments(eventId, page, perPage);
          set({
            judgments: response.data,
            pagination: response.pagination,
            loading: false
          });
        } catch (error: any) {
          set({
            error: error.message || 'Failed to fetch judgments',
            loading: false
          });
        }
      },

      createJudgment: async (judgmentData: CreateJudgmentRequest) => {
        set({ loading: true, error: null });
        
        try {
          const newJudgment = await ApiService.createJudgment(judgmentData);
          
          // Add to current judgments list
          set((state) => ({
            judgments: [newJudgment, ...state.judgments],
            loading: false
          }));
          
          return newJudgment;
        } catch (error: any) {
          set({
            error: error.message || 'Failed to create judgment',
            loading: false
          });
          return null;
        }
      },

      updateJudgment: async (id: string, judgmentData: Partial<CreateJudgmentRequest>) => {
        set({ loading: true, error: null });
        
        try {
          // Note: Update endpoint not implemented in API service yet
          // This would call ApiService.updateJudgment(id, judgmentData)
          
          // For now, just update local state
          set((state) => ({
            judgments: state.judgments.map(judgment => 
              judgment.id === id ? { ...judgment, ...judgmentData } : judgment
            ),
            loading: false
          }));
        } catch (error: any) {
          set({
            error: error.message || 'Failed to update judgment',
            loading: false
          });
        }
      },

      deleteJudgment: async (id: string) => {
        set({ loading: true, error: null });
        
        try {
          // Note: Delete endpoint not implemented in API service yet
          // This would call ApiService.deleteJudgment(id)
          
          // For now, just remove from local state
          set((state) => ({
            judgments: state.judgments.filter(judgment => judgment.id !== id),
            loading: false
          }));
        } catch (error: any) {
          set({
            error: error.message || 'Failed to delete judgment',
            loading: false
          });
        }
      },

      // Filter and sort actions
      setFilters: (filters: Partial<JudgmentFilters>) => {
        set((state) => ({
          filters: { ...state.filters, ...filters }
        }));
      },

      setSortOptions: (sortOptions: JudgmentSortOptions) => {
        set({ sortOptions });
      },

      clearFilters: () => {
        set({ filters: defaultFilters });
      },

      // Utility actions
      setLoading: (loading: boolean) => {
        set({ loading });
      },

      setError: (error: string | null) => {
        set({ error });
      },

      clearError: () => {
        set({ error: null });
      }
    }),
    {
      name: 'judgments-store',
    }
  )
);
