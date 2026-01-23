import React, { useCallback } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';
import Underline from '@tiptap/extension-underline';
import TextAlign from '@tiptap/extension-text-align';
import { TextStyle } from '@tiptap/extension-text-style';
import { Color } from '@tiptap/extension-color';
import Highlight from '@tiptap/extension-highlight';
import Placeholder from '@tiptap/extension-placeholder';
import Youtube from '@tiptap/extension-youtube';
import { Table } from '@tiptap/extension-table';
import { TableRow } from '@tiptap/extension-table-row';
import { TableCell } from '@tiptap/extension-table-cell';
import { TableHeader } from '@tiptap/extension-table-header';
import HorizontalRule from '@tiptap/extension-horizontal-rule';
import Heading from '@tiptap/extension-heading';
import {
    Bold,
    Italic,
    Underline as UnderlineIcon,
    Strikethrough,
    List,
    ListOrdered,
    Link as LinkIcon,
    Image as ImageIcon,
    AlignLeft,
    AlignCenter,
    AlignRight,
    AlignJustify,
    Heading1,
    Heading2,
    Heading3,
    Quote,
    Minus,
    Youtube as YoutubeIcon,
    Table as TableIcon,
    Undo,
    Redo,
    Palette,
    Highlighter,
    Type,
    Trash2
} from 'lucide-react';
import './TipTapEditor.css';

// Color palette
const TEXT_COLORS = [
    '#000000', '#374151', '#dc2626', '#ea580c', '#ca8a04',
    '#16a34a', '#0891b2', '#2563eb', '#7c3aed', '#db2777'
];

const HIGHLIGHT_COLORS = [
    '#fef08a', '#bbf7d0', '#bfdbfe', '#ddd6fe', '#fecdd3',
    '#fed7aa', '#d1d5db', '#a5f3fc', '#f5d0fe', '#fef3c7'
];

const TipTapEditor = ({ content, onChange, onImageUpload, placeholder = 'Nhập nội dung bài viết...' }) => {
    const editor = useEditor({
        extensions: [
            StarterKit.configure({
                heading: false, // We use the Heading extension separately
                horizontalRule: false,
            }),
            Heading.configure({
                levels: [1, 2, 3],
            }),
            Underline,
            Link.configure({
                openOnClick: false,
            }),
            Image.configure({
                inline: true,
                allowBase64: true,
            }),
            TextAlign.configure({
                types: ['heading', 'paragraph'],
            }),
            TextStyle,
            Color,
            Highlight.configure({
                multicolor: true,
            }),
            Placeholder.configure({
                placeholder,
            }),
            Youtube.configure({
                controls: true,
                nocookie: true,
            }),
            Table.configure({
                resizable: true,
            }),
            TableRow,
            TableCell,
            TableHeader,
            HorizontalRule,
        ],
        content: content || '',
        onUpdate: ({ editor }) => {
            onChange(editor.getHTML());
        },
    });

    if (!editor) {
        return <div className="tiptap-loading">Đang tải editor...</div>;
    }

    // Formatting handlers
    const handleBold = () => editor.chain().focus().toggleBold().run();
    const handleItalic = () => editor.chain().focus().toggleItalic().run();
    const handleUnderline = () => editor.chain().focus().toggleUnderline().run();
    const handleStrike = () => editor.chain().focus().toggleStrike().run();

    const handleHeading = (level) => editor.chain().focus().toggleHeading({ level }).run();

    const handleBulletList = () => editor.chain().focus().toggleBulletList().run();
    const handleOrderedList = () => editor.chain().focus().toggleOrderedList().run();

    const handleAlignLeft = () => editor.chain().focus().setTextAlign('left').run();
    const handleAlignCenter = () => editor.chain().focus().setTextAlign('center').run();
    const handleAlignRight = () => editor.chain().focus().setTextAlign('right').run();
    const handleAlignJustify = () => editor.chain().focus().setTextAlign('justify').run();

    const handleBlockquote = () => editor.chain().focus().toggleBlockquote().run();
    const handleHorizontalRule = () => editor.chain().focus().setHorizontalRule().run();

    const handleUndo = () => editor.chain().focus().undo().run();
    const handleRedo = () => editor.chain().focus().redo().run();

    const handleClearFormat = () => editor.chain().focus().clearNodes().unsetAllMarks().run();

    const handleLink = () => {
        const previousUrl = editor.getAttributes('link').href;
        const url = window.prompt('Nhập URL:', previousUrl);

        if (url === null) return;
        if (url === '') {
            editor.chain().focus().extendMarkRange('link').unsetLink().run();
            return;
        }
        editor.chain().focus().extendMarkRange('link').setLink({ href: url, target: '_blank' }).run();
    };

    const handleImage = async () => {
        if (onImageUpload) {
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = 'image/*';
            input.onchange = async (e) => {
                const file = e.target.files[0];
                if (file) {
                    try {
                        const url = await onImageUpload(file);
                        if (url) {
                            editor.chain().focus().setImage({ src: url }).run();
                        }
                    } catch (error) {
                        console.error('Error uploading image:', error);
                        alert('Không thể upload ảnh: ' + error.message);
                    }
                }
            };
            input.click();
        } else {
            const url = window.prompt('Nhập URL hình ảnh:');
            if (url) {
                editor.chain().focus().setImage({ src: url }).run();
            }
        }
    };

    const handleYoutube = () => {
        const url = window.prompt('Nhập URL YouTube:');
        if (url) {
            editor.commands.setYoutubeVideo({ src: url });
        }
    };

    const handleTable = () => {
        editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();
    };

    const handleColor = (color) => {
        editor.chain().focus().setColor(color).run();
    };

    const handleHighlight = (color) => {
        editor.chain().focus().toggleHighlight({ color }).run();
    };

    return (
        <div className="tiptap-editor-container">
            {/* Toolbar Row 1: Text Formatting */}
            <div className="tiptap-toolbar">
                {/* Undo/Redo */}
                <button type="button" className="tiptap-btn" onClick={handleUndo} title="Undo (Ctrl+Z)">
                    <Undo size={16} />
                </button>
                <button type="button" className="tiptap-btn" onClick={handleRedo} title="Redo (Ctrl+Y)">
                    <Redo size={16} />
                </button>
                <div className="tiptap-divider" />

                {/* Headings */}
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('heading', { level: 1 }) ? 'active' : ''}`}
                    onClick={() => handleHeading(1)}
                    title="Heading 1"
                >
                    <Heading1 size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('heading', { level: 2 }) ? 'active' : ''}`}
                    onClick={() => handleHeading(2)}
                    title="Heading 2"
                >
                    <Heading2 size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('heading', { level: 3 }) ? 'active' : ''}`}
                    onClick={() => handleHeading(3)}
                    title="Heading 3"
                >
                    <Heading3 size={16} />
                </button>
                <div className="tiptap-divider" />

                {/* Basic Formatting */}
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('bold') ? 'active' : ''}`}
                    onClick={handleBold}
                    title="Bold (Ctrl+B)"
                >
                    <Bold size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('italic') ? 'active' : ''}`}
                    onClick={handleItalic}
                    title="Italic (Ctrl+I)"
                >
                    <Italic size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('underline') ? 'active' : ''}`}
                    onClick={handleUnderline}
                    title="Underline (Ctrl+U)"
                >
                    <UnderlineIcon size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('strike') ? 'active' : ''}`}
                    onClick={handleStrike}
                    title="Strikethrough"
                >
                    <Strikethrough size={16} />
                </button>
                <div className="tiptap-divider" />

                {/* Text Color */}
                <div className="tiptap-dropdown">
                    <button type="button" className="tiptap-btn" title="Màu chữ">
                        <Palette size={16} />
                    </button>
                    <div className="tiptap-dropdown-content">
                        <div className="tiptap-color-grid">
                            {TEXT_COLORS.map(color => (
                                <button
                                    key={color}
                                    type="button"
                                    className="tiptap-color-btn"
                                    style={{ backgroundColor: color }}
                                    onClick={() => handleColor(color)}
                                />
                            ))}
                        </div>
                    </div>
                </div>

                {/* Highlight */}
                <div className="tiptap-dropdown">
                    <button type="button" className={`tiptap-btn ${editor.isActive('highlight') ? 'active' : ''}`} title="Highlight">
                        <Highlighter size={16} />
                    </button>
                    <div className="tiptap-dropdown-content">
                        <div className="tiptap-color-grid">
                            {HIGHLIGHT_COLORS.map(color => (
                                <button
                                    key={color}
                                    type="button"
                                    className="tiptap-color-btn"
                                    style={{ backgroundColor: color }}
                                    onClick={() => handleHighlight(color)}
                                />
                            ))}
                        </div>
                    </div>
                </div>
                <div className="tiptap-divider" />

                {/* Alignment */}
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive({ textAlign: 'left' }) ? 'active' : ''}`}
                    onClick={handleAlignLeft}
                    title="Căn trái"
                >
                    <AlignLeft size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive({ textAlign: 'center' }) ? 'active' : ''}`}
                    onClick={handleAlignCenter}
                    title="Căn giữa"
                >
                    <AlignCenter size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive({ textAlign: 'right' }) ? 'active' : ''}`}
                    onClick={handleAlignRight}
                    title="Căn phải"
                >
                    <AlignRight size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive({ textAlign: 'justify' }) ? 'active' : ''}`}
                    onClick={handleAlignJustify}
                    title="Căn đều"
                >
                    <AlignJustify size={16} />
                </button>
                <div className="tiptap-divider" />

                {/* Lists */}
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('bulletList') ? 'active' : ''}`}
                    onClick={handleBulletList}
                    title="Danh sách không thứ tự"
                >
                    <List size={16} />
                </button>
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('orderedList') ? 'active' : ''}`}
                    onClick={handleOrderedList}
                    title="Danh sách có thứ tự"
                >
                    <ListOrdered size={16} />
                </button>
                <div className="tiptap-divider" />

                {/* Blockquote & Divider */}
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('blockquote') ? 'active' : ''}`}
                    onClick={handleBlockquote}
                    title="Trích dẫn"
                >
                    <Quote size={16} />
                </button>
                <button type="button" className="tiptap-btn" onClick={handleHorizontalRule} title="Đường kẻ ngang">
                    <Minus size={16} />
                </button>
                <div className="tiptap-divider" />

                {/* Media */}
                <button
                    type="button"
                    className={`tiptap-btn ${editor.isActive('link') ? 'active' : ''}`}
                    onClick={handleLink}
                    title="Chèn link"
                >
                    <LinkIcon size={16} />
                </button>
                <button type="button" className="tiptap-btn" onClick={handleImage} title="Chèn ảnh">
                    <ImageIcon size={16} />
                </button>
                <button type="button" className="tiptap-btn" onClick={handleYoutube} title="Chèn YouTube">
                    <YoutubeIcon size={16} />
                </button>
                <button type="button" className="tiptap-btn" onClick={handleTable} title="Chèn bảng">
                    <TableIcon size={16} />
                </button>
                <div className="tiptap-divider" />

                {/* Clear Formatting */}
                <button type="button" className="tiptap-btn" onClick={handleClearFormat} title="Xoá định dạng">
                    <Trash2 size={16} />
                </button>
            </div>

            {/* Table controls - show when in table */}
            {editor.isActive('table') && (
                <div className="tiptap-table-controls">
                    <button type="button" onClick={() => editor.chain().focus().addColumnBefore().run()}>+ Cột trái</button>
                    <button type="button" onClick={() => editor.chain().focus().addColumnAfter().run()}>+ Cột phải</button>
                    <button type="button" onClick={() => editor.chain().focus().deleteColumn().run()}>Xoá cột</button>
                    <span className="tiptap-table-divider">|</span>
                    <button type="button" onClick={() => editor.chain().focus().addRowBefore().run()}>+ Hàng trên</button>
                    <button type="button" onClick={() => editor.chain().focus().addRowAfter().run()}>+ Hàng dưới</button>
                    <button type="button" onClick={() => editor.chain().focus().deleteRow().run()}>Xoá hàng</button>
                    <span className="tiptap-table-divider">|</span>
                    <button type="button" onClick={() => editor.chain().focus().deleteTable().run()} style={{ color: '#ef4444' }}>Xoá bảng</button>
                </div>
            )}

            {/* Editor Content */}
            <EditorContent editor={editor} className="tiptap-content" />
        </div>
    );
};

export default TipTapEditor;
