/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.erlide.ui.editors.outline;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.erlide.basiccore.ErlLogger;
import org.erlide.basicui.util.StringMatcher;
import org.erlide.ui.ErlideUIMessages;
import org.erlide.ui.actions.SortAction;

/**
 * AbstractInfoPopupDialog
 * 
 */
public class QuickOutlinePopupDialog extends PopupDialog implements
		IInformationControl, IInformationControlExtension,
		IInformationControlExtension2, DisposeListener {

	private TreeViewer fTreeViewer;

	private IOutlineContentCreator fOutlineContentCreator;

	private IOutlineSelectionHandler fOutlineSelectionHandler;

	private Text fFilterText;

	private StringMatcher fStringMatcher;

	private QuickOutlineNamePatternFilter fNamePatternFilter;

	private SortAction fSortAction;

	private ITreeContentProvider fTreeContentProvider;

	private ILabelProvider fTreeLabelProvider;

	private ViewerComparator fTreeViewerComparator;

	private ViewerComparator fTreeViewerDefaultComparator;

	public QuickOutlinePopupDialog(Shell parent, int shellStyle,
			IOutlineContentCreator creator, IOutlineSelectionHandler handler) {
		super(parent, shellStyle, true, true, true, true, null, null);
		// Set outline creator
		fOutlineContentCreator = creator;
		// Set outline handler
		fOutlineSelectionHandler = handler;
		// Initialize the other fields
		initialize();
		// Create all controls early to preserve the life cycle of the original
		// implementation.
		create();
	}

	/**
	 * 
	 */
	private void initialize() {
		setInfoText(ErlideUIMessages.QuickOutlinePopupDialog_infoTextPressEscToExit);

		fFilterText = null;
		fTreeViewer = null;
		fStringMatcher = null;
		fNamePatternFilter = null;
		fSortAction = null;
		fTreeContentProvider = null;
		fTreeLabelProvider = null;
		fTreeViewerComparator = null;
		fTreeViewerDefaultComparator = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.PopupDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		// Applies only to dialog body - not title. See createTitleControl
		// Create an empty dialog area, if the source page is not defined
		if ((fOutlineContentCreator == null) ||
				(fOutlineSelectionHandler == null)) {
			return super.createDialogArea(parent);
		}
		// Create the tree viewer
		createUIWidgetTreeViewer(parent);
		// Add listeners to the tree viewer
		createUIListenersTreeViewer();
		// Create the actions
		createUIActions();
		// Add a dispose listner
		addDisposeListener(this);
		// Return the tree
		return fTreeViewer.getControl();
	}

	/**
	 * 
	 */
	private void createUIActions() {
		// Add sort action to dialog menu
		fSortAction = new SortAction(
				fTreeViewer,
				ErlideUIMessages.PDEMultiPageContentOutline_SortingAction_tooltip,
				fTreeViewerComparator, fTreeViewerDefaultComparator, null,
				false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.PopupDialog#fillDialogMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void fillDialogMenu(IMenuManager dialogMenu) {
		// Add the sort action
		dialogMenu.add(fSortAction);
		// Separator
		dialogMenu.add(new Separator());
		// Add the default actions
		super.fillDialogMenu(dialogMenu);
	}

	/**
	 * @param parent
	 */
	private void createUIWidgetTreeViewer(Composite parent) {

		int style = SWT.H_SCROLL | SWT.V_SCROLL;
		// Create the tree
		Tree widget = new Tree(parent, style);
		// Configure the layout
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = widget.getItemHeight() * 12;
		widget.setLayoutData(data);
		// Create the tree viewer
		fTreeViewer = new TreeViewer(widget);
		// Add the name pattern filter
		fNamePatternFilter = new QuickOutlineNamePatternFilter();
		fTreeViewer.addFilter(fNamePatternFilter);
		// Set the content provider
		fTreeContentProvider = fOutlineContentCreator
				.createOutlineContentProvider();
		fTreeViewer.setContentProvider(fTreeContentProvider);
		// Set the label provider
		fTreeLabelProvider = fOutlineContentCreator
				.createOutlineLabelProvider();
		fTreeViewer.setLabelProvider(fTreeLabelProvider);
		// Create the outline sorter (to be set on the sort action)
		fTreeViewerComparator = fOutlineContentCreator
				.createOutlineComparator();
		// Set the comparator to null (sort action will be disabled initially
		// because of this)
		// Create the default outline sorter (Most like this will just return
		// null to indicate sorting disabled
		fTreeViewerDefaultComparator = fOutlineContentCreator
				.createDefaultOutlineComparator();
		fTreeViewer.setComparator(fTreeViewerDefaultComparator);
		fTreeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		fTreeViewer.setUseHashlookup(true);
		fTreeViewer.setInput(fOutlineContentCreator.getOutlineInput());
	}

	/**
	 * 
	 */
	private void createUIListenersTreeViewer() {
		// Get the underlying tree widget
		final Tree tree = fTreeViewer.getTree();
		// Handle key events
		tree.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.character == 0x1B) {
					// Dispose on ESC key press
					dispose();
				}
			}

			public void keyReleased(KeyEvent e) {
				// NO-OP
			}
		});
		// Handle mouse clicks
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				handleTreeViewerMouseUp(tree, e);
			}
		});
		// Handle mouse move events
		tree
				.addMouseMoveListener(new QuickOutlineMouseMoveListener(
						fTreeViewer));
		// Handle widget selection events
		tree.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				// NO-OP
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				gotoSelectedElement();
			}
		});
	}

	/**
	 * @param tree
	 * @param e
	 */
	private void handleTreeViewerMouseUp(final Tree tree, MouseEvent e) {
		// Ensure a selection was made, the first mouse button was
		// used and the event happened in the tree
		if ((tree.getSelectionCount() < 1) || (e.button != 1) ||
				(tree.equals(e.getSource()) == false)) {
			return;
		}
		// Selection is made in the selection changed listener
		Object object = tree.getItem(new Point(e.x, e.y));
		TreeItem selection = tree.getSelection()[0];
		if (selection.equals(object)) {
			gotoSelectedElement();
		}
	}

	/**
	 * @return
	 */
	private Object getSelectedElement() {
		if (fTreeViewer == null) {
			return null;
		}
		return ((IStructuredSelection) fTreeViewer.getSelection())
				.getFirstElement();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#addDisposeListener(org.eclipse.swt.events.DisposeListener)
	 */
	public void addDisposeListener(DisposeListener listener) {
		getShell().addDisposeListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#addFocusListener(org.eclipse.swt.events.FocusListener)
	 */
	public void addFocusListener(FocusListener listener) {
		getShell().addFocusListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#computeSizeHint()
	 */
	public Point computeSizeHint() {
		// Return the shell's size
		// Note that it already has the persisted size if persisting is enabled.
		return getShell().getSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#dispose()
	 */
	public void dispose() {
		close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#isFocusControl()
	 */
	public boolean isFocusControl() {
		if (fTreeViewer.getControl().isFocusControl() ||
				fFilterText.isFocusControl()) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#removeDisposeListener(org.eclipse.swt.events.DisposeListener)
	 */
	public void removeDisposeListener(DisposeListener listener) {
		getShell().removeDisposeListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#removeFocusListener(org.eclipse.swt.events.FocusListener)
	 */
	public void removeFocusListener(FocusListener listener) {
		getShell().removeFocusListener(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setBackgroundColor(org.eclipse.swt.graphics.Color)
	 */
	public void setBackgroundColor(Color background) {
		applyBackgroundColor(background, getContents());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setFocus()
	 */
	public void setFocus() {
		getShell().forceFocus();
		fFilterText.setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setForegroundColor(org.eclipse.swt.graphics.Color)
	 */
	public void setForegroundColor(Color foreground) {
		applyForegroundColor(foreground, getContents());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setInformation(java.lang.String)
	 */
	public void setInformation(String information) {
		// Ignore
		// See IInformationControlExtension2
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setLocation(org.eclipse.swt.graphics.Point)
	 */
	public void setLocation(Point location) {
		/*
		 * If the location is persisted, it gets managed by PopupDialog - fine.
		 * Otherwise, the location is computed in Window#getInitialLocation,
		 * which will center it in the parent shell / main monitor, which is
		 * wrong for two reasons: - we want to center over the editor / subject
		 * control, not the parent shell - the center is computed via the
		 * initalSize, which may be also wrong since the size may have been
		 * updated since via min/max sizing of
		 * AbstractInformationControlManager. In that case, override the
		 * location with the one computed by the manager. Note that the call to
		 * constrainShellSize in PopupDialog.open will still ensure that the
		 * shell is entirely visible.
		 */
		if ((getPersistBounds() == false) || (getDialogSettings() == null)) {
			getShell().setLocation(location);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setSize(int, int)
	 */
	public void setSize(int width, int height) {
		getShell().setSize(width, height);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setSizeConstraints(int,
	 *      int)
	 */
	public void setSizeConstraints(int maxWidth, int maxHeight) {
		// Ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControl#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			open();
		} else {
			saveDialogBounds(getShell());
			getShell().setVisible(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControlExtension#hasContents()
	 */
	public boolean hasContents() {
		if ((fTreeViewer == null) || (fTreeViewer.getInput() == null)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IInformationControlExtension2#setInput(java.lang.Object)
	 */
	public void setInput(Object input) {
		// Input comes from ErlangSourceInfoProvider.getInformation2()
		// The input should be a model object of some sort
		// Turn it into a structured selection and set the selection in the tree
		if (input != null) {
			fTreeViewer.setSelection(new StructuredSelection(input));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	public void widgetDisposed(DisposeEvent e) {
		// Note: We do not reuse the dialog
		fTreeViewer = null;
		fFilterText = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.PopupDialog#createTitleControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createTitleControl(Composite parent) {
		// Applies only to dialog title - not body. See createDialogArea
		// Create the text widget
		createUIWidgetFilterText(parent);
		// Add listeners to the text widget
		createUIListenersFilterText();
		// Return the text widget
		return fFilterText;
	}

	/**
	 * @param parent
	 * @return
	 */
	private void createUIWidgetFilterText(Composite parent) {
		// Create the widget
		fFilterText = new Text(parent, SWT.NONE);
		// Set the font
		GC gc = new GC(parent);
		gc.setFont(parent.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		// Create the layout
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.CENTER;
		fFilterText.setLayoutData(data);
	}

	/**
	 * 
	 */
	private void gotoSelectedElement() {
		Object selectedElement = getSelectedElement();
		ErlLogger.log("&&>> " + selectedElement);
		if (selectedElement == null) {
			return;
		}
		dispose();
		// Get the content outline page within the content outline view
		// and select the item there to keep the quick outline in sync with the
		// main outline and prevent duplicate selection events from occurring
		fOutlineSelectionHandler.getContentOutline().setSelection(
				new StructuredSelection(selectedElement));
	}

	/**
	 * 
	 */
	private void createUIListenersFilterText() {
		// Handle key events
		fFilterText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 0x0D) {
					// Return key was pressed
					gotoSelectedElement();
				} else if (e.keyCode == SWT.ARROW_DOWN) {
					// Down key was pressed
					fTreeViewer.getTree().setFocus();
				} else if (e.keyCode == SWT.ARROW_UP) {
					// Up key was pressed
					fTreeViewer.getTree().setFocus();
				} else if (e.character == 0x1B) {
					// Escape key was pressed
					dispose();
				}
			}

			public void keyReleased(KeyEvent e) {
				// NO-OP
			}
		});
		// Handle text modify events
		fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String text = ((Text) e.widget).getText();
				int length = text.length();
				if (length > 0) {
					// Append a '*' pattern to the end of the text value if it
					// does not have one already
					if (text.charAt(length - 1) != '*') {
						text = text + '*';
					}
					// Prepend a '*' pattern to the beginning of the text value
					// if it does not have one already
					if (text.charAt(0) != '*') {
						text = '*' + text;
					}
				}
				// Set and update the pattern
				setMatcherString(text, true);
			}
		});
	}

	/**
	 * Sets the patterns to filter out for the receiver.
	 * <p>
	 * The following characters have special meaning: ? => any character * =>
	 * any string
	 * </p>
	 * 
	 * @param pattern
	 *            the pattern
	 * @param update
	 *            <code>true</code> if the viewer should be updated
	 */
	private void setMatcherString(String pattern, boolean update) {
		if (pattern.length() == 0) {
			fStringMatcher = null;
		} else {
			fStringMatcher = new StringMatcher(pattern, true, false);
		}
		// Update the name pattern filter on the tree viewer
		fNamePatternFilter.setStringMatcher(fStringMatcher);
		// Update the tree viewer according to the pattern
		if (update) {
			stringMatcherUpdated();
		}
	}

	/**
	 * The string matcher has been modified. The default implementation
	 * refreshes the view and selects the first matched element
	 */
	private void stringMatcherUpdated() {
		// Refresh the tree viewer to re-filter
		fTreeViewer.getControl().setRedraw(false);
		fTreeViewer.refresh();
		fTreeViewer.expandAll();
		selectFirstMatch();
		fTreeViewer.getControl().setRedraw(true);
	}

	/**
	 * Selects the first element in the tree which matches the current filter
	 * pattern.
	 */
	private void selectFirstMatch() {
		Tree tree = fTreeViewer.getTree();
		Object element = findFirstMatchToPattern(tree.getItems());
		if (element != null) {
			fTreeViewer.setSelection(new StructuredSelection(element), true);
		} else {
			fTreeViewer.setSelection(StructuredSelection.EMPTY);
		}
	}

	/**
	 * @param items
	 * @return
	 */
	private Object findFirstMatchToPattern(TreeItem[] items) {
		// Match the string pattern against labels
		ILabelProvider labelProvider = (ILabelProvider) fTreeViewer
				.getLabelProvider();
		// Process each item in the tree
		for (int i = 0; i < items.length; i++) {
			Object element = items[i].getData();
			// Return the first element if no pattern is set
			if (fStringMatcher == null) {
				return element;
			}
			// Return the element if it matches the pattern
			if (element != null) {
				String label = labelProvider.getText(element);
				if (fStringMatcher.match(label)) {
					return element;
				}
			}
			// Recursively check the elements children for a match
			element = findFirstMatchToPattern(items[i].getItems());
			// Return the child element match if found
			if (element != null) {
				return element;
			}
		}
		// No match found
		return null;
	}

}
