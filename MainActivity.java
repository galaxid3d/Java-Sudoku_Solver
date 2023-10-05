package com.GalaxId.Soduku_Solver;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;
import android.widget.TableRow.*;
import android.text.*;
import android.view.*;
import android.preference.*;
import android.graphics.*;
import android.widget.TextView.*;
import android.view.inputmethod.*;
import android.content.*;
import android.text.style.*;

public class MainActivity extends Activity 
{
	TableLayout tableLayout;
	EditText ResultOut_edt;
	CheckBox isDiagSud_chk;
	NumberPicker Solve_Count_nmPckr;
	int solvesCount = 0; int SolvesCountNeed = 0;
	int[][] enteredSud; int[][] sud;
	int[][][] allowedNums;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		ResultOut_edt = (EditText) findViewById(R.id.ResultOut_edt);
		isDiagSud_chk = (CheckBox) findViewById(R.id.isDiagSud_chk);
		Solve_Count_nmPckr = (NumberPicker) findViewById(R.id.Solve_Count_nmPckr);
			Solve_Count_nmPckr.setMinValue(0);
			Solve_Count_nmPckr.setMaxValue(2147483647);
			Solve_Count_nmPckr.setValue(1);
			Solve_Count_nmPckr.setWrapSelectorWheel(false); //убираем закольцованную прокрутку
		
		tableLayout = (TableLayout) findViewById(R.id.tableLayout);
		for (int j=0; j<11; j++) {
			int m = 0;
			TableRow tableRow = new TableRow(this);
			tableRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			for (int i=0; i<11; i++) {
				final EditText _edt = new EditText(this);
				if ((j == 3)|(j == 7)|(i == 3)|(i == 7)) {
					TextView separator_txt = new TextView(this);
					if ((j == 3)|(j == 7)) {
						separator_txt.setText("_____");
						separator_txt.setMaxHeight(16);
						separator_txt.setId(i);
					}
					else {
						separator_txt.setText("  |");
						separator_txt.setTextSize(20);
						if (i == 3) separator_txt.setId(9);
						else if (i == 7) separator_txt.setId(10);
					}
					separator_txt.setMaxWidth(30);
					separator_txt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)); 
					tableRow.addView(separator_txt, i);
				}
				else {
					_edt.setInputType(InputType.TYPE_CLASS_NUMBER);
					_edt.setMaxLines(1);
					_edt.setSingleLine(true); 
					_edt.setMinWidth(28);
					_edt.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)});
					_edt.setId(m); //нумеруем поля для ввода в строке tableLayout по-порядку с 0 до 8 
					_edt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)); 
					_edt.addTextChangedListener(new TextWatcher() { //обработчик изменения
						public void afterTextChanged(Editable s) { EditText_onEdit(_edt, s.toString()); }
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
						public void onTextChanged(CharSequence s, int start, int before, int count) {}
					});
					_edt.setOnEditorActionListener(new TextView.OnEditorActionListener() { //обработчик для Enter - т.е. переходит на след. ячейку
						@Override
						public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
							int[] editPos = getEditPosInTable((EditText) v);
							if (editPos[1] == 8) {
								editPos[1] = -1;
								editPos[0] = editPos[0] + 1;
								if (editPos[0] == 3) editPos[0] = 4;
								else if (editPos[0] == 7) editPos[0] = 8;
							}
							if (editPos[0]<11) ((EditText) tableLayout.getChildAt(editPos[0]).findViewById(editPos[1]+1)).requestFocus();
							else hideClipBoard();
							return true;
						}
						});if ((i!=3)&(i!=7))
					tableRow.addView(_edt, i);
				}
				if ((i != 3)&(i != 7)) m += 1;
			}
			tableLayout.addView(tableRow, j);
		}
		enteredSud = new int[9][9];
	}
	
	public int validRow(int j, boolean forTable) { //пришлось добавить, т.к. строки таблицы почему-то нельзя пронумеровать также как столбцы: где EditText, там 0-8, где TextView, там 10-11
		if (j < 3) return j;
		if (forTable) {
			if ((j >= 3)&(j < 6)) return j + 1;
			else return j + 2;
		}
		else {
			if ((j >= 4)&(j < 7)) return j - 1;
			else return j - 2;
		}
	}
	
	public void colorize_value(int[] editPos, int value, int old_value, boolean isDiag) { //красит число в красный или чёрный цвет, если оно не повторяется
		//проверяем для введённого значения
		int validRow = validRow(editPos[0], false);
		if (!notInAll(enteredSud, validRow, editPos[1], value, isDiag)) {
			if (inRow(enteredSud, validRow, editPos[1], value))
				for (int i=0; i<9; i++)
					if (enteredSud[validRow][i] == value)
						((EditText) tableLayout.getChildAt(editPos[0]).findViewById(i)).setTextColor(Color.RED);

			if (inColumn(enteredSud, editPos[1], validRow, value))
				for (int j=0; j<9; j++)
					if (enteredSud[j][editPos[1]] == value)
						((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(editPos[1])).setTextColor(Color.RED);
			
			if (inSquare(enteredSud, validRow, editPos[1], value)) {
				int[] nm = squareIndex(validRow, editPos[1]);
				for (int j=nm[0]; j<nm[0]+3; j++)
					for (int i=nm[1]; i<nm[1]+3; i++)
						if (enteredSud[j][i] == value)
							((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(i)).setTextColor(Color.RED);
			}
			
			if (isDiag) 
				if (inDiag(enteredSud, validRow, editPos[1], value)) {
					if (validRow == editPos[1])
						for (int j=0; j<9; j++) 
							if (enteredSud[j][j] == value)
								((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(j)).setTextColor(Color.RED);
					if (8-validRow == editPos[1])
						for (int j=0; j<9; j++) 
							if (enteredSud[j][8-j] == value)
								((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(8-j)).setTextColor(Color.RED);
				}
		}
		else ((EditText) tableLayout.getChildAt(editPos[0]).findViewById(editPos[1])).setTextColor(Color.BLACK);
		
		//проверяем для старого значения(т.е. исправляем цвета у тех цифр,которые перестали повторяться в строке/столбце/квадрате/диагонали)
		for (int i=0; i<9; i++)
			if (enteredSud[validRow][i] == old_value)
				if (notInAll(enteredSud, validRow, i, old_value, isDiag))
					((EditText) tableLayout.getChildAt(editPos[0]).findViewById(i)).setTextColor(Color.BLACK);
				
		for (int j=0; j<9; j++)
			if (enteredSud[j][editPos[1]] == old_value)
				if (notInAll(enteredSud, j, editPos[1], old_value, isDiag))
					((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(editPos[1])).setTextColor(Color.BLACK);
		
		int[] nm = squareIndex(validRow, editPos[1]);
		for (int j=nm[0]; j<nm[0]+3; j++)
			for (int i=nm[1]; i<nm[1]+3; i++)
				if (enteredSud[j][i] == old_value)
					if (notInAll(enteredSud, j, i, old_value, isDiag))
						((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(i)).setTextColor(Color.BLACK);
					
		if (isDiag) {
			if (validRow == editPos[1])
				for (int j=0; j<9; j++) 
					if (enteredSud[j][j] == old_value)
						if (notInAll(enteredSud, j, j, old_value, isDiag))
							((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(j)).setTextColor(Color.BLACK);
			if (8-validRow == editPos[1])
				for (int j=0; j<9; j++)
					if (enteredSud[j][8-j] == old_value)
						if (notInAll(enteredSud, j, 8-j, old_value, isDiag))
							((EditText) tableLayout.getChildAt(validRow(j, true)).findViewById(8-j)).setTextColor(Color.BLACK);
		}
	}
	
	public void EditText_onEdit(EditText v, String s) {
		int[] editPos = getEditPosInTable(v);
		int validRow = validRow(editPos[0], false);
		if ("_123456789".indexOf(v.getText().toString()) > 0) { //пришлось добавить "_" вначало, т.к. почему-то при пустом EditText всё равно возвращает 0, как если бы там была еденица
			int value = Integer.parseInt(v.getText().toString());
			int old_value = enteredSud[validRow][editPos[1]];
			if (value != old_value) {
				enteredSud[validRow][editPos[1]] = value;
				colorize_value(editPos, value, old_value, isDiagSud_chk.isChecked());
			}
		}
		else {
			int old_value = enteredSud[validRow][editPos[1]];
			enteredSud[validRow][editPos[1]] = 0;
			if (0 != old_value) colorize_value(editPos, -1, old_value, isDiagSud_chk.isChecked());
		}
	}
	
	public int[] getEditPosInTable(EditText v) { //возвращает номер строки и столбца данного EditText в таблице
		int[] result = new int[2]; result[0] = result[1] = -1;
		for (int j=0; j<11; j++) { //для строк почему-то не получается использовать findViewById, если tableRow присваивать соответствующий id
			for (int i=0; i<9; i++)
				if (tableLayout.getChildAt(j).findViewById(i) == v) {
					result[0] = j;
					result[1] = i;
					break;
				}
			if (result[0] == j) break;
		}
		return result;
	}
	
	public void hideClipBoard() {
		ResultOut_edt.requestFocus();
		InputMethodManager hide_clipboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		hide_clipboard.hideSoftInputFromWindow(ResultOut_edt.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}
	
	public void Solve_Start(View v) {
		ResultOut_edt.setText("");
		hideClipBoard();
		if (isValidSud(enteredSud, isDiagSud_chk.isChecked())) {
			sud = null; sud = new int[9][9];
			for (int j=0; j<9; j++)
				for (int i=0; i<9; i++)
					sud[j][i] = enteredSud[j][i];
			SolvesCountNeed = Solve_Count_nmPckr.getValue();
			solvesCount = 0;
			if (SolvesCountNeed != 0) {
				if (createAllowedNums(isDiagSud_chk.isChecked()))
					Sudoku_Solver(0, 0, isDiagSud_chk.isChecked());
			}
			if (solvesCount == 0 ) ResultOut_edt.setText("Решений нет");
		}
		else ResultOut_edt.setText("Судоку неверное:"+'\n'+"есть повторение"+'\n'+"чисел");
	}
	
	boolean inRow(int[][] sud, int j, int m, int v) {
		for (int i=0; i<m; i++)
			if (sud[j][i] == v) return true;
		for (int i=m+1; i<9; i++)
			if (enteredSud[j][i] == v) return true;
		return false;
	}
	
	boolean inColumn(int[][] sud, int i, int k, int v) {
		for (int j=0; j<k; j++)
			if (sud[j][i] == v) return true;
		for (int j=k+1; j<9; j++)
			if (enteredSud[j][i] == v) return true;
		return false;
	}
	
	int[] squareIndex(int j, int i) {
		if ((j<3)&(i<3)) {return new int[] {0,0};}
		else if ((j<3)&(i>=3)&(i<6)) {return new int[] {0,3};}
		else if ((j<3)&(i>=6)) {return new int[] {0,6};}
		else if ((j>=3)&(j<6)&(i<3)) {return new int[] {3,0};}
		else if ((j>=3)&(j<6)&(i>=3)&(i<6)) {return new int[] {3,3};}
		else if ((j>=3)&(j<6)&(i>=6)) {return new int[] {3,6};}
		else if ((j>=6)&(i<3)) {return new int[] {6,0};}
		else if ((j>=6)&(i>=3)&(i<6)) {return new int[] {6,3};}
		else {return new int[] {6,6};}
	}
	
	boolean inSquare(int[][] sud, int k, int m, int v) {
		int[] nm = squareIndex(k, m);
		int l;
		for (int j=k; j<nm[0]+3; j++) {
			if (j == k) l = m + 1;
			else l = nm[1];
			for (int i=l; i<nm[1]+3; i++)
				if (enteredSud[j][i] == v) return true;
		}
		for (int j=nm[0]; j<nm[0]+3; j++)
			for (int i=nm[1]; i<nm[1]+3; i++)
				if ((j==k)&(i==m)) return false; //когда дошли до проверяемой ячейки
				else if (sud[j][i] == v) return true;
		return false;
	}
	
	boolean inDiag(int[][] sud, int k, int m, int v) {
		if ((k == m)|(8-k == m)) {
			if (k == m) {
				for (int j=0; j<k; j++) 
					if (sud[j][j] == v) return true;
				for (int j=k+1; j<9; j++) 
					if (enteredSud[j][j] == v) return true;
			}
			if (8-k == m) {
				for (int j=0; j<k; j++) 
					if (sud[j][8-j] == v) return true;
				for (int j=k+1; j<9; j++) 
					if (enteredSud[j][8-j] == v) return true;
			}
		}
		return false;
	}
	
	boolean notInAll(int[][] sud, int j, int i, int v, boolean isDiagonal) {
		if ((!inRow(sud, j, i, v))&(!inColumn(sud, i, j, v))&(!inSquare(sud, j ,i, v)))
			if (isDiagonal) {
				if (!inDiag(sud, j, i, v)) return true;}
			else return true;
		return false;
	}
	
	public boolean createAllowedNums(boolean isDiag) {
		allowedNums = null; allowedNums = new int[9][9][9];
		for (int j=0; j<9; j++)
			for (int i=0; i<9; i++) {
				int allowedCount = 0;
				for (int v=1; v<10; v++)
					if (notInAll(enteredSud, j, i, v, isDiag)) {
						allowedNums[j][i][allowedCount] = v;
						allowedCount += 1;
					}
				if (allowedCount == 0) return false;
			}
		return true;
	}
	
	boolean Sudoku_Solver(int k, int m, boolean isDiagonal) {
		if (m == 9) {
			k+=1;
			m=0;
		}	
		if (k == 9) {
			solvesCount+=1;
			printSud(sud, solvesCount);
			return true;
		}
		int l;
		for (int j=k; j<9; j++) {
			if (j == k) l = m;
			else l = 0;
			for (int i=l; i<9; i++)
				if (enteredSud[j][i] == 0) {
					for (int v=0; v<9; v++) //тут все доступные из allowedNums
						if (allowedNums[j][i][v] == 0) break;
						else if (notInAll(sud, j, i, allowedNums[j][i][v], isDiagonal)) {
								sud[j][i] = allowedNums[j][i][v];				
								if (Sudoku_Solver(j, i+1, isDiagonal))
									if (solvesCount >= SolvesCountNeed) return true;
							}
					return solvesCount >= SolvesCountNeed;
				}
				else if ((j == 8)&(i == 8)) {
					solvesCount+=1;
					printSud(sud, solvesCount);
					return true;
				}
		}
		return solvesCount >= SolvesCountNeed;
	}
	
	boolean isValidSud(int[][] sud, boolean isDiag) {
		for (int j=0; j<9; j++)
			for (int i=0; i<9; i++) if (sud[j][i] != 0)
				if (!notInAll(sud, j, i, sud[j][i], isDiag))
					return false;
		return true;
	}
	
	void printSud(int[][] sud, int k) {
		String tmp_s = "";
		if (k>1) tmp_s += "\n\n";
		tmp_s += '('+Integer.toString(k)+')'+'\n';
		for (int j=0; j<9; j++) {
			for (int i=0; i<9; i++) {
				tmp_s += sud[j][i] + " ";
				if (((i+1)%3 == 0)&(i != 8)) {tmp_s += " ";}
			}
			tmp_s += '\n';
			if (((j+1)%3 == 0)&(j != 8)) {tmp_s += '\n';}
		}
		ResultOut_edt.setText(ResultOut_edt.getText().toString()+tmp_s);
		ResultOut_edt.setSelection(ResultOut_edt.getText().length());
	}
}
