﻿package com.negset.macaron;

import java.io.File;
import java.io.FilenameFilter;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * 選曲画面の動作・描画を行うクラス.
 *
 * @author negset
 */
public class StateSelect extends BasicGameState
{
	/** ゲーム画面のID */
	private int id;

	/** シーン管理用変数 */
	private int scene;
	/** 曲選択中を表すシーン定数 */
	private static final int SELECT_MUSIC = 0;
	/** 難易度選択中を表すシーン定数 */
	private static final int SELECT_LEVEL = 1;
	/** 選択完了を表すシーン定数 */
	private static final int SELECTED = 2;

	/** 背景画像 */
	private Image bg;
	/** フレーム画像 */
	private Image frame;
	/** オートプレイ表記用の画像 */
	private Image autoplayIcon;
	/** 難易度選択の画像 */
	private Image[] levelCard;

	/** カードをあらかじめ生成し,貯めておくための配列 */
	private Card[] card;
	/** mbpディレクトリ */
	private File mbpDir;
	/** 譜面ディレクトリ */
	private File[] mbp;
	/** 曲選択カーソルの位置 */
	private int musicCsrPos;
	/** 難易度選択カーソルの位置 */
	private int levelCsrPos;
	/** 選択された譜面ディレクトリのパス */
	public static String mbpPath;
	/** 背景アニメーション用のx座標 */
	private float bgX1, bgX2;
	/** カード選択アニメーション用のカウント */
	private float animeCnt;

	/**
	 * コンストラクタ
	 *
	 * @param id ゲーム画面のID
	 */
	StateSelect(int id)
	{
		this.id = id;
	}

	/**
	 * ゲーム画面の初期化.
	 * リソースファイルの読み込み等を行う.
	 * 起動時に一度だけ呼ばれる.
	 */
	@Override
	public void init(GameContainer gc, StateBasedGame game)
			throws SlickException
	{
		bg = new Image("res\\select\\bg.png");
		frame = new Image("res\\select\\frame.png");
		autoplayIcon = new Image("res\\select\\autoplay_icon.png");
		levelCard = new Image[4];
		levelCard[0] = new Image("res\\select\\level_easy.png");
		levelCard[1] = new Image("res\\select\\level_normal.png");
		levelCard[2] = new Image("res\\select\\level_hard.png");
		levelCard[3] = new Image("res\\select\\level_lunatic.png");
		card = new Card[99];
		for (int i = 0; i < card.length; i++)
		{
			card[i] = new Card();
		}
		musicCsrPos = 0;
		levelCsrPos = 0;
	}

	/**
	 * 描画処理を行う.
	 * 1ループにつき1回呼ばれる.
	 */
	@Override
	public void render(GameContainer gc, StateBasedGame game, Graphics g)
			throws SlickException
	{
		if (Loading.isFinished())
		{
			bg.draw(bgX1, 0);
			bg.draw(bgX2, 0);

			// 難易度カードを描画する.
			for (int i = 0; i < 4; i++)
			{
				int x = 345;
				if (i == levelCsrPos)
				{
					x += 15;
				}
				levelCard[i].setAlpha(animeCnt / 50);
				levelCard[i].draw(x, 312 + 93 * i - animeCnt * 2f);
			}

			// カードを描画する.
			for (int i = 0; i < mbp.length; i++)
			{
				if (i != musicCsrPos)
				{
					card[i].draw(g);
				}
			}
			// カーソル位置のカードは最前面に描画する.
			if (mbp.length != 0)
			{
				card[musicCsrPos].draw(g);
			}

			frame.draw();
			if (StatePlay.autoplay)
			{
				int w = autoplayIcon.getWidth();
				int h = autoplayIcon.getHeight();
				autoplayIcon.draw(720 - w / 2, 540 - h / 2);
			}

			// FPSを描画する.
			Drawer.drawFps(gc.getFPS(), g);
		}
		else
		{
			Loading.draw(g);
		}
	}

	/**
	 * 動作を規定する.
	 * 1ループにつき1回呼ばれる.
	 */
	@Override
	public void update(GameContainer gc, StateBasedGame game, int delta)
			throws SlickException
	{
		if (Loading.isFinished())
		{
			//背景画像をアニメーションさせる.
			bgX1 += 0.035 * delta;
			if (bgX1 >= 800)
			{
				bgX1 -= 800;
			}
			else if (bgX1 > 0)
			{
				bgX2 = bgX1 - 800;
			}
			else
			{
				bgX2 = bgX1 + 800;
			}

			switch (scene)
			{
				case SELECT_MUSIC:
					updateSelectMusic(delta);
					break;

				case SELECT_LEVEL:
					updateSelectLevel(delta);
					break;

				case SELECTED:
					game.enterState(Main.STATE_PLAY,
							new FadeOutTransition(), new FadeInTransition());
			}
		}
		else
		{
			Loading.loadResorce();
		}
	}

	/**
	 * 曲選択中の動作を規定する.
	 */
	private void updateSelectMusic(int delta)
	{
		keySelectMusic();

		if (animeCnt > 0)
		{
			animeCnt -= delta;
			if (animeCnt < 0)
			{
				animeCnt = 0;
			}
		}

		// カードを移動させる.
		float cx = 400 - musicCsrPos * 210;
		for (int i = 0; i < mbp.length; i++)
		{
			card[i].move(cx, (i == musicCsrPos), delta);
			cx += 210;
		}
	}

	/**
	 * 難易度選択中の動作を規定する.
	 */
	private void updateSelectLevel(int delta)
	{
		keySelectLevel();

		if (animeCnt < 100)
		{
			animeCnt += delta;
			if (animeCnt > 100)
			{
				animeCnt = 100;
			}
		}

		// カードを移動させる.
		for (int i = 0; i < mbp.length; i++)
		{
			card[i].move(i - musicCsrPos, delta);
		}
	}

	/**
	 * フィールドの初期化等を行う.
	 * プレイ画面に移行した時に1度だけ呼ばれる.
	 */
	@Override
	public void enter(GameContainer gc, StateBasedGame game)
			throws SlickException
	{
		scene = 0;
		mbpDir = new File("mbp");
		if (!mbpDir.exists())
		{
			mbpDir.mkdir();
		}
		mbp = mbpDir.listFiles(new Filter());
		float cx = 400 - musicCsrPos * 210;
		for (int i = 0; i < mbp.length; i++)
		{
			card[i].activate(mbp[i].getPath(), cx);
			cx += 210;
		}
		animeCnt = 0;
	}

	/**
	 * 曲選択中のキーの状態に応じた処理を行う.
	 */
	private void keySelectMusic()
	{
		Key.load();
		if (Key.isPressed(Key.ENTER))
		{
			Drawer.playSE(Drawer.SE_ENTER);
			scene = SELECT_LEVEL;
		}
		else if (Key.isPressed(Key.LEFT))
		{
			if (musicCsrPos > 0)
			{
				Drawer.playSE(Drawer.SE_CURSOR);
				musicCsrPos--;
			}
		}
		else if (Key.isPressed(Key.RIGHT))
		{
			if (musicCsrPos < mbp.length-1)
			{
				Drawer.playSE(Drawer.SE_CURSOR);
				musicCsrPos++;
			}
		}
	}

	/**
	 * 難易度選択中のキーの状態に応じた処理を行う.
	 */
	private void keySelectLevel()
	{
		Key.load();
		if (Key.isPressed(Key.ENTER))
		{
			Drawer.playSE(Drawer.SE_ENTER);
			mbpPath = mbp[musicCsrPos].getPath();
			scene = SELECTED;
		}
		else if (Key.isPressed(Key.BACK))
		{
			scene = SELECT_MUSIC;
		}
		else if (Key.isPressed(Key.UP))
		{
			if (levelCsrPos > 0)
			{
				Drawer.playSE(Drawer.SE_CURSOR);
				levelCsrPos--;
			}
		}
		else if (Key.isPressed(Key.DOWN))
		{
			if (levelCsrPos < 3)
			{
				Drawer.playSE(Drawer.SE_CURSOR);
				levelCsrPos++;
			}
		}
	}

	/**
	 * ゲーム画面のIDを返す.
	 *
	 * @return ゲーム画面のID
	 */
	@Override
	public int getID()
	{
		return id;
	}
}

/**
 * 選曲画面に表示する
 * 曲カードの動作・描画を行うクラス.
 *
 * @author negset
 */
class Card
{
	/** インスタンス有効フラグ */
	public boolean active;
	/** 背景画像 */
	private Image card;
	/** 曲サムネイル画像 */
	private Image thumb;
	/** 曲名画像 */
	private Image title;
	/** アーティスト名画像 */
	private Image artist;
	/** x座標 */
	private float x;
	/** 描画の縮小率 */
	private float scale;

	/**
	 * コンストラクタ
	 */
	Card()
	{
		// 生成時はインスタンスは有効ではない.
		active = false;
	}

	/**
	 * 曲選択中の動作を規定する.
	 *
	 * @param cx 移動先のx座標
	 * @param focus カーソル位置であるかのフラグ
	 * @param delta 前のループにかかった時間(単位:ミリ秒)
	 */
	public void move(float cx, boolean focus, int delta)
	{
		// カードを移動する.
		if (x < cx)
		{
			x += 1.5 * delta;
			if (x > cx)
			{
				x = cx;
			}
		}
		else if (x > cx)
		{
			x -= 1.5 * delta;
			if (x < cx)
			{
				x = cx;
			}
		}
		// 縮小率を変える.
		if (focus)
		{
			if (scale < 1)
			{
				scale += 0.002 * delta;
				if (scale > 1)
				{
					scale = 1;
				}
			}
		}
		else
		{
			if (scale > 0.7)
			{
				scale -= 0.002 * delta;
				if (scale < 0.7)
				{
					scale = 0.7f;
				}
			}
		}
	}

	/**
	 * 難易度選択中の動作を規定する.
	 *
	 * @param dIndex フォーカス中のカードに対する相対位置
	 * @param delta 前のループにかかった時間(単位:ミリ秒)
	 */
	public void move(int dIndex, int delta)
	{
		// 選択中のカードより左のカード
		if (dIndex < 0)
		{
			if (x > -card.getWidth())
			{
				x -= 1.5 * delta;
			}
		}
		// 選択中のカード
		else if (dIndex == 0)
		{
			if (x > 180)
			{
				x -= 1.5 * delta;
				if (x < 185)
				{
					x = 185;
				}
			}
		}
		// 選択中のカードより右のカード
		else
		{
			if (x < 800 + card.getWidth())
			{
				x += 1.5 * delta;
			}
		}
	}

	/**
	 * 描画処理を行う.
	 *
	 * @param g 描画先
	 */
	public void draw(Graphics g)
	{
		float w1 = card.getWidth() * scale;
		float h1 = card.getHeight() * scale;
		card.draw(x - w1/2, 300 - h1/2, scale);

		float w2 = thumb.getWidth() * scale;
		thumb.draw(x - w2/2, 300 - h1/2 + 12*scale, scale);

		float w3 = title.getWidth() * scale;
		title.draw(x - w3/2, 300 - h1/2 + 300*scale, scale);

		float w4 = artist.getWidth() * scale;
		artist.draw(x - w4/2, 300 - h1/2 + 330*scale, scale);
	}

	/**
	 * インスタンスの有効化を行う.
	 * インスタンスの使い回しをしているので,初期化処理もここで行う.
	 *
	 * @param mbpPath 譜面ディレクトリのパス
	 * @param x 座標のx成分
	 * @throws SlickException
	 */
	public void activate(String mbpPath, float x)
			throws SlickException
	{
		active = true;

		card = new Image("res\\select\\card.png");
		thumb = new Image(mbpPath + "\\thumbnail.png");
		title = new Image(mbpPath + "\\title.png");
		artist = new Image(mbpPath + "\\artist.png");

		this.x = x;
		scale = 0.7f;
	}
}

/**
 * 譜面ディレクトリを選別するクラス.
 *
 * @author negset
 */
class Filter implements FilenameFilter
{
	/**
	 * ディレクトリが与えられた場合のみtrueを返す.
	 */
	@Override
	public boolean accept(File file, String name)
	{
		if (file.isDirectory())
		{
			return true;
		}
		return false;
	}
}