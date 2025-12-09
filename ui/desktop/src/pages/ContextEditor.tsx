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
import { ContextTemplate, CreateContextRequest } from '@/types/contexts';
import { getEmoji } from '@/utils/emojiMapping';
import { fetchEntityNames, type EntityNamesCache } from '@/utils/entityNames';

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
    selectedTemplateForEdit,
    setSelectedTemplateForEdit,
    clearTemplateEdit
  } = useNavigationStore();
  const [templates, setTemplates] = useState<ContextTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [entityNames, setEntityNames] = useState<EntityNamesCache | null>(null);
  
  // Template creation form state
  const [formData, setFormData] = useState<CreateContextRequest>({
    name: '',
    description: '',
    category_id: undefined,
    forma_id: undefined,
    cause_id: undefined,
    develop_id: undefined,
    effect_id: undefined,
  });
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [duplicateError, setDuplicateError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Load templates and entity names on mount
  useEffect(() => {
    loadTemplates();
    loadEntityNames();
  }, []);

  // Template selection flow: Show selection UI when flag is set
  useEffect(() => {
    if (selectTemplateForEvent) {
      loadTemplates();
    }
  }, [selectTemplateForEvent]);

  // Load template for editing - observe selectedTemplateForEdit and prefill form
  useEffect(() => {
    if (selectedTemplateForEdit) {
      setFormData({
        name: selectedTemplateForEdit.name,
        description: selectedTemplateForEdit.description || '',
        category_id: selectedTemplateForEdit.categoryId,
        forma_id: selectedTemplateForEdit.formaId,
        cause_id: selectedTemplateForEdit.causeId,
        develop_id: selectedTemplateForEdit.developId,
        effect_id: selectedTemplateForEdit.effectId,
      });
      setShowCreateForm(true);
      // Clear selection after form is populated (Android LaunchedEffect pattern)
      clearTemplateEdit();
    }
  }, [selectedTemplateForEdit, clearTemplateEdit]);

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

  const loadEntityNames = async () => {
    try {
      const names = await fetchEntityNames();
      setEntityNames(names);
    } catch (error) {
      console.error('Failed to load entity names:', error);
    }
  };

  const handleCreateTemplate = () => {
    setShowCreateForm(true);
    setFormData({
      name: '',
      description: '',
      category_id: undefined,
      forma_id: undefined,
      cause_id: undefined,
      develop_id: undefined,
      effect_id: undefined,
    });
    setFormErrors({});
    clearTemplateEdit();
  };

  const handleEditTemplate = (template: ContextTemplate) => {
    // Set selectedTemplateForEdit in navigation store (Android onTemplateClick pattern)
    setSelectedTemplateForEdit({
      id: template.id,
      name: template.name,
      description: template.description,
      categoryId: template.category_id,
      formaId: template.forma_id,
      causeId: template.cause_id,
      developId: template.develop_id,
      effectId: template.effect_id,
    });
    // Form will be prefilled via useEffect observing selectedTemplateForEdit
  };

  const handleTemplateClick = (template: ContextTemplate) => {
    // Android pattern: Clicking template card navigates to form with prefilled fields
    // This creates a new template based on the selected one (not editing existing)
    handleEditTemplate(template);
  };

  const handleDeleteTemplate = async (_templateId: number) => {
    if (!confirm('Are you sure you want to delete this template?')) {
      return;
    }
    // TODO: Implement delete functionality when API is available
    alert('Delete functionality not yet implemented');
  };

  const handleFormChange = (field: keyof CreateContextRequest, value: string | number | undefined) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (formErrors[field]) {
      setFormErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
    // Clear duplicate error when form data changes (Android pattern)
    if (duplicateError) {
      setDuplicateError(null);
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};
    
    // Name is required (Android rule)
    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    }

    // All context fields are required (Android rule)
    if (formData.category_id === undefined || formData.category_id === null) {
      newErrors.category_id = 'Category is required';
    }
    if (formData.forma_id === undefined || formData.forma_id === null) {
      newErrors.forma_id = 'Forma is required';
    }
    if (formData.cause_id === undefined || formData.cause_id === null) {
      newErrors.cause_id = 'Cause is required';
    }
    if (formData.develop_id === undefined || formData.develop_id === null) {
      newErrors.develop_id = 'Develop is required';
    }
    if (formData.effect_id === undefined || formData.effect_id === null) {
      newErrors.effect_id = 'Effect is required';
    }

    setFormErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Check if form is valid for save button state (Android rule: all fields must be filled)
  const isFormValid = formData.name.trim() && 
    formData.category_id !== undefined && formData.category_id !== null &&
    formData.forma_id !== undefined && formData.forma_id !== null &&
    formData.cause_id !== undefined && formData.cause_id !== null &&
    formData.develop_id !== undefined && formData.develop_id !== null &&
    formData.effect_id !== undefined && formData.effect_id !== null &&
    Object.keys(formErrors).length === 0;

  const handleSaveTemplate = async () => {
    if (!validateForm()) {
      return;
    }

    // Check for duplicates before save (Android ContextTemplateEditorScreen.kt pattern)
    setSaving(true);
    setDuplicateError(null);
    
    try {
      // Check for duplicate using API service
      const isDuplicate = await ApiService.checkDuplicateTemplate(
        {
          category_id: formData.category_id,
          forma_id: formData.forma_id,
          cause_id: formData.cause_id,
          develop_id: formData.develop_id,
          effect_id: formData.effect_id,
        },
        selectedTemplateForEdit?.id
      );

      if (isDuplicate) {
        // Display error message and prevent save (Android 409 Conflict pattern)
        setDuplicateError('Template with identical fields already exists (409 Conflict)');
        setSaving(false);
        return;
      }

      // Android rule: Always create new template (editing existing templates redirects to create new)
      // Even if selectedTemplateForEdit is set, we create a new template with prefilled values
      await ApiService.createContext(formData);
      alert('Template created successfully');
      setShowCreateForm(false);
      setFormData({
        name: '',
        description: '',
        category_id: undefined,
        forma_id: undefined,
        cause_id: undefined,
        develop_id: undefined,
        effect_id: undefined,
      });
      setDuplicateError(null);
      clearTemplateEdit();
      await loadTemplates();
    } catch (error: unknown) {
      const errorMsg = error instanceof Error ? error.message : 'Failed to save template';
      if (errorMsg.includes('409') || errorMsg.includes('identical')) {
        setDuplicateError('Template with identical fields already exists (409 Conflict)');
      } else {
        alert(`Failed to save template: ${errorMsg}`);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleCancelForm = () => {
    setShowCreateForm(false);
    setFormData({
      name: '',
      description: '',
      category_id: undefined,
      forma_id: undefined,
      cause_id: undefined,
      develop_id: undefined,
      effect_id: undefined,
    });
    setFormErrors({});
    clearTemplateEdit();
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
    // Note: clearTemplateSelection is called in NewEvent.tsx after template is applied
    onNavigate('new-event');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {getEmoji('screens', 'contextEditor')} Context Templates
      </h1>

      {/* Create Template Button */}
      <div className="mb-6">
        <button
          onClick={handleCreateTemplate}
          className="px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600"
        >
          {getEmoji('actions', 'create')} Create Template
        </button>
      </div>

      {selectTemplateForEvent && (
        <div className="bg-blue-50 dark:bg-blue-900 border border-blue-200 dark:border-blue-700 rounded-lg p-4 mb-6">
          <p className="text-blue-800 dark:text-blue-200">
            {getEmoji('status', 'info')} Select a template to prefill event form fields
          </p>
        </div>
      )}

      {/* Create/Edit Template Form */}
      {showCreateForm && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            {selectedTemplateForEdit ? getEmoji('actions', 'edit') : getEmoji('actions', 'create')} {selectedTemplateForEdit ? 'Edit' : 'Create'} Template
          </h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Name {getEmoji('fields', 'name')} *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => handleFormChange('name', e.target.value)}
                className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                  formErrors.name ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                }`}
                placeholder="Template name"
              />
              {formErrors.name && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{formErrors.name}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Description {getEmoji('fields', 'description')}
              </label>
              <textarea
                value={formData.description || ''}
                onChange={(e) => handleFormChange('description', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-gray-100"
                placeholder="Template description"
                rows={3}
              />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Category {getEmoji('fields', 'category')} <span className="text-red-500">*</span>
                </label>
                <select
                  value={formData.category_id || ''}
                  onChange={(e) => handleFormChange('category_id', e.target.value ? parseInt(e.target.value) : undefined)}
                  className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                    formErrors.category_id ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                  }`}
                  required
                >
                  <option value="">Select Category</option>
                  {entityNames?.categories.map((cat: { id: number; name: string }) => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
                {formErrors.category_id && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{formErrors.category_id}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Forma {getEmoji('fields', 'forma')} <span className="text-red-500">*</span>
                </label>
                <select
                  value={formData.forma_id || ''}
                  onChange={(e) => handleFormChange('forma_id', e.target.value ? parseInt(e.target.value) : undefined)}
                  className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                    formErrors.forma_id ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                  }`}
                  required
                >
                  <option value="">Select Forma</option>
                  {entityNames?.formas.map((forma: { id: number; name: string }) => (
                    <option key={forma.id} value={forma.id}>{forma.name}</option>
                  ))}
                </select>
                {formErrors.forma_id && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{formErrors.forma_id}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Cause {getEmoji('fields', 'cause')} <span className="text-red-500">*</span>
                </label>
                <select
                  value={formData.cause_id || ''}
                  onChange={(e) => handleFormChange('cause_id', e.target.value ? parseInt(e.target.value) : undefined)}
                  className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                    formErrors.cause_id ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                  }`}
                  required
                >
                  <option value="">Select Cause</option>
                  {entityNames?.causes.map((cause: { id: number; name: string }) => (
                    <option key={cause.id} value={cause.id}>{cause.name}</option>
                  ))}
                </select>
                {formErrors.cause_id && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{formErrors.cause_id}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Develop {getEmoji('fields', 'develop')} <span className="text-red-500">*</span>
                </label>
                <select
                  value={formData.develop_id || ''}
                  onChange={(e) => handleFormChange('develop_id', e.target.value ? parseInt(e.target.value) : undefined)}
                  className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                    formErrors.develop_id ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                  }`}
                  required
                >
                  <option value="">Select Develop</option>
                  {entityNames?.develops.map((develop: { id: number; name: string }) => (
                    <option key={develop.id} value={develop.id}>{develop.name}</option>
                  ))}
                </select>
                {formErrors.develop_id && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{formErrors.develop_id}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Effect {getEmoji('fields', 'effect')} <span className="text-red-500">*</span>
                </label>
                <select
                  value={formData.effect_id || ''}
                  onChange={(e) => handleFormChange('effect_id', e.target.value ? parseInt(e.target.value) : undefined)}
                  className={`w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:text-gray-100 ${
                    formErrors.effect_id ? 'border-red-500' : 'border-gray-300 dark:border-gray-600'
                  }`}
                  required
                >
                  <option value="">Select Effect</option>
                  {entityNames?.effects.map((effect: { id: number; name: string }) => (
                    <option key={effect.id} value={effect.id}>{effect.name}</option>
                  ))}
                </select>
                {formErrors.effect_id && (
                  <p className="mt-1 text-sm text-red-600 dark:text-red-400">{formErrors.effect_id}</p>
                )}
              </div>
            </div>
            {/* Duplicate Error Display */}
            {duplicateError && (
              <div className="bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-700 rounded-lg p-4">
                <p className="text-red-800 dark:text-red-200 text-sm">
                  {getEmoji('status', 'error')} {duplicateError}
                </p>
              </div>
            )}
            <div className="flex space-x-4">
              <button
                onClick={handleSaveTemplate}
                disabled={!isFormValid || saving || !!duplicateError}
                className="px-4 py-2 bg-green-600 dark:bg-green-500 text-white rounded hover:bg-green-700 dark:hover:bg-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {getEmoji('actions', 'save')} {saving ? 'Saving...' : 'Save'}
              </button>
              <button
                onClick={handleCancelForm}
                disabled={saving}
                className="px-4 py-2 bg-gray-600 dark:bg-gray-500 text-white rounded hover:bg-gray-700 dark:hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {getEmoji('actions', 'cancel')} Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Templates List */}
      {loading ? (
        <div className="text-center py-8 text-gray-700 dark:text-gray-300">{getEmoji('status', 'syncing')} Loading templates...</div>
      ) : templates.length === 0 ? (
        <div className="text-center py-8 text-gray-500 dark:text-gray-400">{getEmoji('status', 'warning')} No templates found</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {templates.map((template) => (
            <div
              key={template.id}
              className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700 p-6 cursor-pointer hover:shadow-lg transition-shadow"
              onClick={() => handleTemplateClick(template)}
            >
              <div className="flex items-start justify-between mb-2">
                <h3 className="font-semibold text-gray-900 dark:text-gray-100">{template.name}</h3>
                <div className="flex space-x-2">
                  <button
                    onClick={() => handleEditTemplate(template)}
                    className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300"
                    title="Edit template"
                  >
                    {getEmoji('actions', 'edit')}
                  </button>
                  <button
                    onClick={() => handleDeleteTemplate(template.id)}
                    className="text-red-600 dark:text-red-400 hover:text-red-800 dark:hover:text-red-300"
                    title="Delete template"
                  >
                    {getEmoji('actions', 'delete')}
                  </button>
                </div>
              </div>
              {template.description && (
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">{template.description}</p>
              )}
              {selectTemplateForEvent ? (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleTemplateSelect(template);
                  }}
                  className="w-full mt-2 px-4 py-2 bg-blue-600 dark:bg-blue-500 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600"
                >
                  {getEmoji('actions', 'select')} Select
                </button>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

