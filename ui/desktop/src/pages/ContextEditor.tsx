import React, { useState, useEffect } from 'react';
import { ApiService } from '@/services/api';
import { useToast } from '@/components/system/Toaster';
import { CreateContextRequest, ContextTemplate } from '@/types/contexts';
import { useContextEditorStore } from '@/stores/contextEditor';
import { useNavigationStore } from '@/stores/navigation';
import { useTemplateContextStore } from '@/stores/templateContext';
import { Screen } from '@/components/layout/TopMenuBar';
import { ContextPicker } from '@/components/context/ContextPicker';
import { getEntityNameById } from '@/utils/entityResolution';
import { validateTemplate } from '@/utils/validation';
import { t } from '@/i18n';

interface NavigationState {
  eventId?: number;
  [key: string]: any;
}

interface ContextEditorProps {
  onNavigate?: (screen: Screen, state?: NavigationState) => void;
}

interface KnowledgeBaseEntity {
  id: number;
  name: string;
}

export const ContextEditor: React.FC<ContextEditorProps> = ({ onNavigate }) => {
  const { addToast } = useToast();
  const { prefilledData: storePrefilledData, setPrefilledData } = useContextEditorStore();
  const { selectTemplateForEvent, setSelectTemplateForEvent } = useNavigationStore();
  const { setSelectedTemplateContext } = useTemplateContextStore();
  
  // Mode: 'list' or 'editor'
  const [mode, setMode] = useState<'list' | 'editor'>('list');
  const [templates, setTemplates] = useState<ContextTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<ContextTemplate | null>(null);
  
  // Form state for editor
  const [formData, setFormData] = useState<CreateContextRequest>({
    name: '',
    category_id: undefined,
    forma_id: undefined,
    cause_id: undefined,
    develop_id: undefined,
    effect_id: undefined,
    description: '',
  });
  
  // Validation errors
  const [nameError, setNameError] = useState<string | null>(null);
  const [categoryError, setCategoryError] = useState<string | null>(null);
  const [formaError, setFormaError] = useState<string | null>(null);
  const [causeError, setCauseError] = useState<string | null>(null);
  const [developError, setDevelopError] = useState<string | null>(null);
  const [effectError, setEffectError] = useState<string | null>(null);
  const [duplicateError, setDuplicateError] = useState<string | null>(null);

  // Knowledge base entities (TODO: Fetch from Tauri commands when available)
  const categories: KnowledgeBaseEntity[] = [];
  const formas: KnowledgeBaseEntity[] = [];
  const causes: KnowledgeBaseEntity[] = [];
  const develops: KnowledgeBaseEntity[] = [];
  const effects: KnowledgeBaseEntity[] = [];

  // Load templates list
  useEffect(() => {
    if (mode === 'list') {
      fetchTemplates();
    }
  }, [mode]);

  // Prefill form when template is selected or store prefilled data exists
  useEffect(() => {
    if (mode === 'editor') {
      if (selectedTemplate) {
        // Prefill from selected template
        setFormData({
          name: '',
          category_id: selectedTemplate.category_id,
          forma_id: selectedTemplate.forma_id,
          cause_id: selectedTemplate.cause_id,
          develop_id: selectedTemplate.develop_id,
          effect_id: selectedTemplate.effect_id,
          description: selectedTemplate.description || '',
        });
        // Clear validation errors
        clearValidationErrors();
      } else if (storePrefilledData) {
        // Prefill from store (when creating from event)
        setFormData({
          name: storePrefilledData.name || '',
          category_id: storePrefilledData.category_id,
          forma_id: storePrefilledData.forma_id,
          cause_id: storePrefilledData.cause_id,
          develop_id: storePrefilledData.develop_id,
          effect_id: storePrefilledData.effect_id,
          description: '',
        });
        setPrefilledData(null); // Clear after using
        clearValidationErrors();
      }
    }
  }, [mode, selectedTemplate, storePrefilledData, setPrefilledData]);

  const fetchTemplates = async () => {
    setLoadingTemplates(true);
    try {
      const response = await ApiService.getContexts();
      setTemplates(response.data || []);
      } catch (error: any) {
      console.error('Failed to fetch templates:', error);
      addToast({
        type: 'error',
        title: t('contexts.errorLoading'),
        message: t('contexts.errorLoadingMessage')
      });
    } finally {
      setLoadingTemplates(false);
    }
  };

  const clearValidationErrors = () => {
    setNameError(null);
    setCategoryError(null);
    setFormaError(null);
    setCauseError(null);
    setDevelopError(null);
    setEffectError(null);
    setDuplicateError(null);
  };

  const handleTemplateClick = (template: ContextTemplate) => {
    if (selectTemplateForEvent) {
      // Store template context for event creation
      setSelectedTemplateContext({
        categoryId: template.category_id || null,
        formaId: template.forma_id || null,
        causeId: template.cause_id || null,
        developId: template.develop_id || null,
        effectId: template.effect_id || null,
      });
      setSelectTemplateForEvent(false);
      // Navigate back to NewEvent
      onNavigate?.('new-event');
    } else {
      // Prefill editor with template data
      setSelectedTemplate(template);
      setMode('editor');
    }
  };

  const handleAddTemplate = () => {
    setSelectedTemplate(null);
    setFormData({
      name: '',
      category_id: undefined,
      forma_id: undefined,
      cause_id: undefined,
      develop_id: undefined,
      effect_id: undefined,
      description: '',
    });
    clearValidationErrors();
    setMode('editor');
  };

  const handleBackToList = () => {
    setMode('list');
    setSelectedTemplate(null);
    clearValidationErrors();
  };

  const handleSave = async () => {
    // Clear previous errors
    clearValidationErrors();

    // Validate using validation utility
    const validationResult = validateTemplate({
      name: formData.name,
      categoryId: formData.category_id ?? null,
      formaId: formData.forma_id ?? null,
      causeId: formData.cause_id ?? null,
      developId: formData.develop_id ?? null,
      effectId: formData.effect_id ?? null,
    }, (key: string) => t(key));

    if (!validationResult.isValid) {
      // Map validation errors to form fields
      validationResult.errors.forEach(error => {
        switch (error.field) {
          case 'name':
            setNameError(error.message);
            break;
          case 'categoryId':
            setCategoryError(error.message);
            break;
          case 'formaId':
            setFormaError(error.message);
            break;
          case 'causeId':
            setCauseError(error.message);
            break;
          case 'developId':
            setDevelopError(error.message);
            break;
          case 'effectId':
            setEffectError(error.message);
            break;
        }
      });
      
      // Show first error in toast
      if (validationResult.errors.length > 0) {
        addToast({
          type: 'error',
          title: t('validation.validationError'),
          message: validationResult.errors[0].message
        });
      }
      return;
    }

    setLoading(true);

    try {
      await ApiService.createContext(formData);
      addToast({
        type: 'success',
        title: t('contexts.templateCreated'),
        message: t('contexts.templateCreatedMessage').replace('{name}', formData.name)
      });
      handleBackToList();
      fetchTemplates(); // Refresh list
    } catch (error: any) {
      console.error('Failed to create context template:', error);
      const errorMessage = error?.response?.data || error?.message || 'Unknown error';
      
      // Check for duplicate error (409 Conflict) - match Android error message exactly
      if (error?.response?.status === 409 || errorMessage.includes('already exists') || errorMessage.includes('duplicate') || errorMessage.includes('409 Conflict')) {
        // Use exact Android error message: "Template with identical fields already exists (409 Conflict)"
        const androidErrorMsg = errorMessage.includes('(409 Conflict)') 
          ? errorMessage 
          : 'Template with identical fields already exists (409 Conflict)';
        setDuplicateError(androidErrorMsg);
        addToast({
          type: 'error',
          title: t('contexts.templateDuplicateError'),
          message: androidErrorMsg
        });
      } else {
        addToast({
          type: 'error',
          title: t('contexts.errorCreating'),
          message: errorMessage || t('contexts.errorCreatingMessage')
        });
      }
    } finally {
      setLoading(false);
    }
  };

  // Context field display for templates (using entityResolution)
  const getContextFieldDisplay = (template: ContextTemplate) => {
    const fields: string[] = [];
    
    if (template.category_id) {
      const name = getEntityNameById(template.category_id, categories, (e) => e.id, (e) => e.name);
      fields.push(`${t('contexts.category')}: ${name || template.category_id}`);
    }
    if (template.forma_id) {
      const name = getEntityNameById(template.forma_id, formas, (e) => e.id, (e) => e.name);
      fields.push(`${t('contexts.forma')}: ${name || template.forma_id}`);
    }
    if (template.cause_id) {
      const name = getEntityNameById(template.cause_id, causes, (e) => e.id, (e) => e.name);
      fields.push(`${t('contexts.cause')}: ${name || template.cause_id}`);
    }
    if (template.develop_id) {
      const name = getEntityNameById(template.develop_id, develops, (e) => e.id, (e) => e.name);
      fields.push(`${t('contexts.develop')}: ${name || template.develop_id}`);
    }
    if (template.effect_id) {
      const name = getEntityNameById(template.effect_id, effects, (e) => e.id, (e) => e.name);
      fields.push(`${t('contexts.effect')}: ${name || template.effect_id}`);
    }
    
    return fields;
  };

  // List Mode: Context Templates Screen
  if (mode === 'list') {
    return (
      <div className="min-h-screen bg-gray-50">
        {/* Top App Bar */}
        <header className="bg-white shadow-sm border-b">
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center py-4">
              <h1 className="text-2xl font-bold text-gray-900">{t('contexts.title')}</h1>
              <button
                onClick={handleAddTemplate}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                {t('contexts.addTemplate')}
              </button>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {loadingTemplates ? (
            <div className="flex items-center justify-center min-h-64">
              <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
            </div>
          ) : templates.length === 0 ? (
            <div className="bg-white rounded-lg shadow p-12 text-center">
              <p className="text-gray-600 mb-4">{t('contexts.noTemplates')}</p>
              <button
                onClick={handleAddTemplate}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                {t('contexts.addFirstTemplate')}
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {templates.map((template) => {
                const contextFields = getContextFieldDisplay(template);
                return (
                  <div
                    key={template.id}
                    onClick={() => handleTemplateClick(template)}
                    className="bg-white rounded-lg shadow p-6 hover:shadow-md transition-shadow cursor-pointer"
                  >
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">{template.name}</h3>
                    {contextFields.length > 0 && (
                      <div className="flex flex-wrap gap-2 mb-2">
                        {contextFields.map((field, idx) => (
                          <span
                            key={idx}
                            className="inline-flex items-center px-3 py-1 rounded-full text-sm bg-gray-100 text-gray-800"
                          >
                            {field}
                          </span>
                        ))}
                      </div>
                    )}
                    {template.description && (
                      <p className="text-sm text-gray-600 mt-2">{template.description}</p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </main>
      </div>
    );
  }

  // Editor Mode: New Template Screen
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top App Bar */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <h1 className="text-2xl font-bold text-gray-900">{t('contexts.newTemplate')}</h1>
            <div className="flex items-center space-x-2">
              <button
                onClick={handleSave}
                disabled={loading}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
              >
                {loading ? t('common.saving') : t('common.save')}
              </button>
              <button
                onClick={handleBackToList}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
              >
                {t('common.cancel')}
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white rounded-lg shadow p-6 space-y-6">
          {duplicateError && (
            <div className="px-4 py-3 bg-red-50 border border-red-200 rounded text-sm text-red-800">
              <strong>{t('common.error')}:</strong> {duplicateError}
            </div>
          )}

          {/* Name Field */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('contexts.templateName')} *
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => {
                setFormData({ ...formData, name: e.target.value });
                if (nameError) setNameError(null);
              }}
              className={`w-full px-3 py-2 border rounded ${
                nameError ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder={t('contexts.templateNamePlaceholder')}
              required
            />
            {nameError && (
              <p className="mt-1 text-sm text-red-600">{nameError}</p>
            )}
          </div>

          {/* Context Fields */}
          <div>
            <h3 className="text-sm font-medium text-gray-700 mb-4">{t('contexts.contextFields')}</h3>
            <div className="space-y-4">
              <ContextPicker
                label={t('contexts.category')}
                value={formData.category_id}
                onChange={(value) => {
                  setFormData({ ...formData, category_id: value });
                  if (categoryError) setCategoryError(null);
                }}
                required
                error={categoryError || undefined}
              />
              <ContextPicker
                label={t('contexts.forma')}
                value={formData.forma_id}
                onChange={(value) => {
                  setFormData({ ...formData, forma_id: value });
                  if (formaError) setFormaError(null);
                }}
                required
                error={formaError || undefined}
              />
              <ContextPicker
                label={t('contexts.cause')}
                value={formData.cause_id}
                onChange={(value) => {
                  setFormData({ ...formData, cause_id: value });
                  if (causeError) setCauseError(null);
                }}
                required
                error={causeError || undefined}
              />
              <ContextPicker
                label={t('contexts.develop')}
                value={formData.develop_id}
                onChange={(value) => {
                  setFormData({ ...formData, develop_id: value });
                  if (developError) setDevelopError(null);
                }}
                required
                error={developError || undefined}
              />
              <ContextPicker
                label={t('contexts.effect')}
                value={formData.effect_id}
                onChange={(value) => {
                  setFormData({ ...formData, effect_id: value });
                  if (effectError) setEffectError(null);
                }}
                required
                error={effectError || undefined}
              />
            </div>
          </div>

          {/* Description Field */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              {t('contexts.description')}
            </label>
            <textarea
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded"
              rows={4}
              placeholder={t('contexts.descriptionPlaceholder')}
            />
          </div>

          {/* Info Note */}
          <div className="bg-blue-50 border border-blue-200 rounded p-3 text-sm text-blue-800">
            <strong>{t('common.note')}:</strong> {t('contexts.duplicateNote')}
          </div>
        </div>
      </main>
    </div>
  );
};
