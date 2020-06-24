package de.adito.git.gui.dialogs;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.adito.git.api.IDiscardable;
import de.adito.git.api.IRepository;
import de.adito.git.api.data.IFileStatus;
import de.adito.git.api.data.diff.EConflictSide;
import de.adito.git.api.data.diff.IFileDiff;
import de.adito.git.api.data.diff.IMergeData;
import de.adito.git.api.prefs.IPrefStore;
import de.adito.git.gui.dialogs.results.IMergeConflictResolutionDialogResult;
import de.adito.git.gui.rxjava.ObservableListSelectionModel;
import de.adito.git.gui.sequences.MergeConflictSequence;
import de.adito.git.gui.swing.ComponentResizeListener;
import de.adito.git.gui.swing.MergeDiffTableCellRenderer;
import de.adito.git.gui.tablemodels.MergeDiffStatusModel;
import de.adito.git.impl.Util;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera 25.10.2018
 */
class MergeConflictDialog extends AditoBaseDialog<Object> implements IDiscardable
{

  private static final String NO_REPO_ERROR_MSG = Util.getResource(MergeConflictDialog.class, "noValidRepoMsg");
  private static final String PREF_STORE_SIZE_KEY = Util.getResource(MergeConflictResolutionDialog.class, "mergePanelSizeKey");

  private final IDialogProvider dialogProvider;
  private final IDialogDisplayer.IDescriptor isValidDescriptor;
  private final IRepository repository;
  private final Subject<List<IMergeData>> mergeConflictDiffs;
  private final JTable mergeConflictTable = new JTable();
  private final JButton manualMergeButton = new JButton("Manual Merge");
  private final JButton acceptYoursButton = new JButton("Accept Yours");
  private final JButton acceptTheirsButton = new JButton("Accept Theirs");
  private final Observable<Optional<List<IMergeData>>> selectedMergeDiffObservable;
  private final MergeDiffStatusModel mergeDiffStatusModel;
  private final Disposable disposable;
  private final Disposable selectionDisposable;
  private final ObservableListSelectionModel observableListSelectionModel;
  private final IPrefStore prefStore;

  @Inject
  MergeConflictDialog(IPrefStore pPrefStore, IDialogProvider pDialogProvider, @Assisted IDialogDisplayer.IDescriptor pIsValidDescriptor,
                      @Assisted Observable<Optional<IRepository>> pRepository, @Assisted List<IMergeData> pMergeConflictDiffs, @Assisted boolean pOnlyConflicting)
  {
    prefStore = pPrefStore;
    dialogProvider = pDialogProvider;
    isValidDescriptor = pIsValidDescriptor;
    repository = pRepository.blockingFirst().orElseThrow(() -> new RuntimeException(NO_REPO_ERROR_MSG));
    observableListSelectionModel = new ObservableListSelectionModel(mergeConflictTable.getSelectionModel());
    mergeConflictTable.setSelectionModel(observableListSelectionModel);
    mergeConflictDiffs = BehaviorSubject.createDefault(pMergeConflictDiffs);
    Observable<Optional<IFileStatus>> obs = repository.getStatus();
    Observable<List<IMergeData>> mergeDiffListObservable = Observable
        .combineLatest(obs, mergeConflictDiffs, (pStatus, pMergeDiffs) -> pMergeDiffs.stream()
            .filter(pMergeDiff -> !pOnlyConflicting || pStatus
                .map(IFileStatus::getConflicting)
                .orElse(Collections.emptySet())
                .stream().anyMatch(pFilePath -> IFileDiff.isSameFile(pFilePath, pMergeDiff.getDiff(EConflictSide.YOURS))))
            .collect(Collectors.toList()));
    selectedMergeDiffObservable = Observable
        .combineLatest(observableListSelectionModel.selectedRows(), mergeDiffListObservable, (pSelectedRows, pMergeDiffList) -> {
          // if either the list or selection is null, more than one element is selected or the list has 0 elements
          if (pSelectedRows == null || pMergeDiffList == null || pMergeDiffList.isEmpty())
          {
            return Optional.empty();
            // if no element is selected (and at least one element is in the list, this follows from the if above)
          }
          List<IMergeData> selectedMergeDiffs = new ArrayList<>();
          for (Integer pSelectedRow : pSelectedRows)
          {
            if (pSelectedRow < pMergeDiffList.size())
              selectedMergeDiffs.add(pMergeDiffList.get(pSelectedRow));
          }
          return Optional.of(selectedMergeDiffs);
        });
    selectionDisposable = selectedMergeDiffObservable.subscribe(pSelectedMergeDiffs -> {
      manualMergeButton.setEnabled(pSelectedMergeDiffs.map(pList -> pList.size() == 1).orElse(false));
      acceptYoursButton.setEnabled(pSelectedMergeDiffs.map(pList -> !pList.isEmpty()).orElse(false));
      acceptTheirsButton.setEnabled(pSelectedMergeDiffs.map(pList -> !pList.isEmpty()).orElse(false));
    });
    disposable = mergeDiffListObservable.subscribe(pList -> isValidDescriptor.setValid(pList.isEmpty()));
    mergeDiffStatusModel = new MergeDiffStatusModel(mergeDiffListObservable);
    _initGui();
  }

  private void _initGui()
  {
    setLayout(new BorderLayout(5, 10));
    setPreferredSize(ComponentResizeListener._getPreferredSize(prefStore, PREF_STORE_SIZE_KEY, new Dimension(800, 600)));
    addComponentListener(new ComponentResizeListener(prefStore, PREF_STORE_SIZE_KEY));
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
    manualMergeButton.addActionListener(e -> _doManualResolve(selectedMergeDiffObservable));
    acceptYoursButton.addActionListener(e -> MergeConflictSequence.acceptDefaultVersion(selectedMergeDiffObservable, EConflictSide.YOURS, repository));
    acceptTheirsButton.addActionListener(e -> MergeConflictSequence.acceptDefaultVersion(selectedMergeDiffObservable, EConflictSide.THEIRS, repository));
    manualMergeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) manualMergeButton.getMaximumSize().getHeight()));
    acceptYoursButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) acceptYoursButton.getMaximumSize().getHeight()));
    acceptTheirsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) acceptTheirsButton.getMaximumSize().getHeight()));
    buttonPanel.setPreferredSize(new Dimension(120, 60));
    buttonPanel.add(manualMergeButton);
    buttonPanel.add(Box.createVerticalStrut(10));
    buttonPanel.add(acceptYoursButton);
    buttonPanel.add(Box.createVerticalStrut(5));
    buttonPanel.add(acceptTheirsButton);
    buttonPanel.add(Box.createVerticalStrut(Integer.MAX_VALUE));
    add(buttonPanel, BorderLayout.EAST);
    mergeConflictTable.setModel(mergeDiffStatusModel);
    mergeConflictTable.getColumnModel().getColumn(mergeDiffStatusModel.findColumn("Filename")).setPreferredWidth(230);
    mergeConflictTable.getColumnModel().getColumn(mergeDiffStatusModel.findColumn("Filepath")).setPreferredWidth(230);
    mergeConflictTable.getColumnModel().getColumn(mergeDiffStatusModel.findColumn("Your Changes")).setPreferredWidth(120);
    mergeConflictTable.getColumnModel().getColumn(mergeDiffStatusModel.findColumn("Their Changes")).setPreferredWidth(120);
    mergeConflictTable.setDefaultRenderer(Object.class, new MergeDiffTableCellRenderer());
    JScrollPane mergeConflictTableScrollPane = new JScrollPane(mergeConflictTable);
    add(mergeConflictTableScrollPane, BorderLayout.CENTER);
  }

  /**
   * @param pSelectedMergeDiffObservable Observable optional list (should be of size 1) of IMergeDatas whose conflicts should be manually resolved
   */
  private void _doManualResolve(Observable<Optional<List<IMergeData>>> pSelectedMergeDiffObservable)
  {
    Optional<List<IMergeData>> mergeDiffOptional = pSelectedMergeDiffObservable.blockingFirst();
    mergeDiffOptional.ifPresent(pMergeDatas -> {
      if (pMergeDatas.size() == 1)
      {
        IMergeConflictResolutionDialogResult<?, ?> result = dialogProvider.showMergeConflictResolutionDialog(pMergeDatas.get(0));
        if (result.isAcceptChanges())
        {
          MergeConflictSequence.acceptManualVersion(pMergeDatas.get(0), repository);
          _removeFromMergeConflicts(pMergeDatas.get(0));
        }
        else if (result.getSelectedButton().equals(IDialogDisplayer.EButtons.ACCEPT_YOURS))
        {
          MergeConflictSequence.acceptDefaultVersion(selectedMergeDiffObservable, EConflictSide.YOURS, repository);
          pMergeDatas.forEach(this::_removeFromMergeConflicts);
        }
        else if (result.getSelectedButton().equals(IDialogDisplayer.EButtons.ACCEPT_THEIRS))
        {
          MergeConflictSequence.acceptDefaultVersion(selectedMergeDiffObservable, EConflictSide.THEIRS, repository);
          pMergeDatas.forEach(this::_removeFromMergeConflicts);
        }
        else
          pMergeDatas.get(0).reset();
      }
    });
  }

  private void _removeFromMergeConflicts(IMergeData pMergeData)
  {
    List<IMergeData> mergeDiffs = mergeConflictDiffs.blockingFirst();
    mergeDiffs.remove(pMergeData);
    mergeConflictDiffs.onNext(mergeDiffs);
  }

  @Override
  public void discard()
  {
    disposable.dispose();
    selectionDisposable.dispose();
    mergeDiffStatusModel.discard();
    observableListSelectionModel.discard();
  }

  @Override
  public String getMessage()
  {
    return null;
  }

  @Override
  public Object getInformation()
  {
    return null;
  }
}
