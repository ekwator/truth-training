/**
 * Context Editor Screen
 * Matches Android Context Templates screen layout and behavior.
 * Implements template selection and creation flows.
 * Route: context-editor
 */

import React, { useEffect, useState } from 'react';
import { Screen } from '@/components/layout/TopMenuBar';
import { useNavigationStore } from '@/stores/navigation';
import { ApiService } from '@/services/api';
import { ContextTemplate } from '@/types/contexts';
import { getEmoji } from '@/utils/emojiMapping';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface ContextEditorProps {
  onNavigate: (screen: Screen, state?: NavigationState) => void;
}

export const ContextEditor: React.FC<ContextEditorProps> = ({ onNavigate }) => {
  const { 
    selectTemplateForEvent,
    setSelectedTemplateContext,
    clearTemplateSelection 
  } = useNavigationStore();
  const [templates, setTemplates] = useState<ContextTemplate[]>([]);
  const [loading, setLoading] = useState(false);

  // Template selection flow: Show selection UI when flag is set
  useEffect(() => {
    if (selectTemplateForEvent) {
      loadTemplates();
    }
  }, [selectTemplateForEvent]);

  const loadTemplates = async () => {
    setLoading(true);
    try {
      const response = await ApiService.getContexts();
      setTemplates(response.data || []);
    } catch (error) {
      console.error('Failed to load templates:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleTemplateSelect = (template: ContextTemplate) => {
    // Android algorithm step 5: Store template context
    setSelectedTemplateContext({
      categoryId: template.category_id,
      formaId: template.forma_id,
      causeId: template.cause_id,
      developId: template.develop_id,
      effectId: template.effect_id,
    });
    
    // Android algorithm step 6: Navigate back (equivalent to popBackStack)
    clearTemplateSelection();
    onNavigate('new-event');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'contextEditor')} Context Templates
      </h1>

      {selectTemplateForEvent && (
        <div className="bg-blue-50 dark:bg-blue-900 border border-blue-200 dark:border-blue-700 rounded-lg p-4 mb-6">
          <p className="text-blue-800 dark:text-blue-200">
            {getEmoji('status', 'info')} Select a template to prefill event form fields
          </p>
        </div>
      )}

      {loading ? (
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">{getEmoji('status', 'syncing')} Loading templates...</div>
      ) : templates.length === 0 ? (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">{getEmoji('status', 'warning')} No templates found</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {templates.map((template) => (
            <div
              key={template.id}
              onClick={() => handleTemplateSelect(template)}
              className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 cursor-pointer hover:shadow-md dark:hover:shadow-gray-600 transition-shadow"
            >
              <h3 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">{template.name}</h3>
              {template.description && (
                <p className="text-sm text-gray-600 dark:text-gray-400">{template.description}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

