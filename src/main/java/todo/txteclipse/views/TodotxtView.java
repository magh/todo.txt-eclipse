package todo.txteclipse.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.todotxt.todotxtjava.Task;
import com.todotxt.todotxtjava.TaskHelper;
import com.todotxt.todotxtjava.TodoUtil;

public class TodotxtView extends ViewPart {

	public static final String ID = TodotxtView.class.getSimpleName();

	private static final File TODOTXTFILE = new File("todo.txt");

	private List<Task> tasks = new ArrayList<Task>();

	private TableViewer viewer;
	// TODO actions: complete, prioritize, update, delete, share
	private final List<Action> actions = new ArrayList<Action>();
	private Action doubleClickAction;

	public TodotxtView() {
	}

	@Override
	public void createPartControl(final Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setComparator(new TaskComparator());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(viewer.getControl(), "todo.txt-eclipse.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		final MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				TodotxtView.this.fillContextMenu(manager);
			}
		});
		final Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(final IMenuManager manager) {
		for (final Action action : actions) {
			manager.add(action);
			manager.add(new Separator());
		}
	}

	private void fillContextMenu(final IMenuManager manager) {
		for (final Action action : actions) {
			manager.add(action);
		}
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(final IToolBarManager manager) {
		for (final Action action : actions) {
			manager.add(action);
		}
	}

	private void makeActions() {
		final ISelection selection = viewer.getSelection();
		final Task task = (Task) ((IStructuredSelection) selection)
				.getFirstElement();

		// complete
		addAction("Complete", new Action() {
			@Override
			public void run() {
				if (showConfirmMessage("Are you sure you want to complete task?")) {
					TaskHelper.setCompleted(task);
					TodoUtil.writeToFile(tasks, TODOTXTFILE);
				}
			}
		});

		// delete
		addAction("Delete", new Action() {
			@Override
			public void run() {
				if (showConfirmMessage("Are you sure you want to delete task?")) {
					final Task find = TaskHelper.find(tasks, task);
					TaskHelper.remove(tasks, find);
					TodoUtil.writeToFile(tasks, TODOTXTFILE);
				}
			}
		});

		// double click
		doubleClickAction = new Action() {
			@Override
			public void run() {
				showMessage(TaskHelper.toFileFormat(task));
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private boolean showConfirmMessage(final String message) {
		return MessageDialog.openConfirm(viewer.getControl().getShell(),
				"Confirm", message);
	}

	private void showMessage(final String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(),
				"Message", message);
	}

	private void addAction(final String text, final Action action) {
		action.setText(text);
		action.setToolTipText(text + " tooltip");
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		actions.add(action);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	class ViewContentProvider implements IStructuredContentProvider {
		@Override
		public void inputChanged(final Viewer v, final Object oldInput,
				final Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(final Object parent) {
			try {
				tasks = TodoUtil.loadTasksFromFile(TODOTXTFILE);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return tasks.toArray();
		}
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		@Override
		public String getColumnText(final Object obj, final int index) {
			final Task task = (Task) obj;
			return TaskHelper.toFileFormat(task);
		}

		@Override
		public Image getColumnImage(final Object obj, final int index) {
			return getImage(obj);
		}

		@Override
		public Image getImage(final Object obj) {
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	class TaskComparator extends ViewerComparator {
		@Override
		public int compare(final Viewer viewer, final Object obj1,
				final Object obj2) {
			final Task task1 = (Task) obj1;
			final Task task2 = (Task) obj2;
			return TaskHelper.byPrio.compare(task1, task2);
		}
	}

}
